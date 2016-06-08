### The Anatomy of an Onyx Program

In this tutorial, we'll take an in-depth view of what's happening when you execute a simple Onyx program. All of the code can be found in the [Onyx Starter repository](https://github.com/onyx-platform/onyx-starter) if you'd like to follow along. The code uses the development environment with ZooKeeper running in memory, so you don't need additional dependencies to run the example for yourself on your machine.

#### The Workflow

At the core of the program is the workflow - the flow of data that we ingest, apply transformations to, and send to an output for storage. In this program, we're going to ingest some sentences from an input source, split the sentence into individual words, play with capitalization, and add a suffix. Finally, we'll send the transformed data to an output source.

Let's examine the workflow pictorially:

![Basic](http://i.imgur.com/uJEenZq.png)

Notice that when we get to the interleaving of case case, we *fork* the computation into two distinct streams. This demonstrates a fundamental ability of Onyx to model directed, acyclic graphs. Let's add another level of detail and show some actual data passing through each stage:

![Detailed](http://i.imgur.com/VMueGgh.png)

A few things to note:
- This diagram shows data *as it's leaving* each transformation.
- Onyx only allows *maps* to be ingested and emitted. These are called *segments*.
- Onyx multiple segments to be emitted in a vector, as seen in the `split by spaces` transformation.
- After `split by spaces`, the diagram only shows `{:word "really"}` moving through the workflow. This is simply for saving space. All words move through the rest of the workflow in the same manner.

All of this is expressed in Clojure as the following form:

```clojure
[[:in :split-by-spaces]
 [:split-by-spaces :mixed-case]
 [:mixed-case :loud]
 [:mixed-case :question]
 [:loud :loud-output]
 [:question :question-output]]
```

#### The Catalog

Now that we've outlined the flow of data for our program in the workflow, let's bind the names we used in the workflow to entries in the catalog. The catalog is used to set up context for functions that will eventually execute on the cluster. We'll look at a few of the catalog entries next.

##### `in`

```clojure
{:onyx/name :in
 :onyx/tenancy-ident :core.async/read-from-chan
 :onyx/type :input
 :onyx/medium :core.async
 :onyx/max-peers 1
 :onyx/batch-size batch-size
 :onyx/doc "Reads segments from a core.async channel"}
```

The first entry in the catalog specifies the input for the workflow. Here, we make use of the core.async plugin for Onyx to make local development convenient. `ident`, `type`, and `medium` are specified by the plugin and should be copied directly. `max-peers` creates an upper bound on the number of peers, and it is 1 here to constrain the task to a single peer. `batch-size` is a performance tuning parameter. `name` corresponds to `:in` in the workflow that we specified above.

##### `split-by-spaces`

```clojure
{:onyx/name :split-by-spaces
 :onyx/fn :onyx-starter.core/split-by-spaces
 :onyx/type :function
 :onyx/batch-size batch-size}
```

Now we turn our attention to the `split-by-spaces` function. Again, the name corresponds to that which is used in the workflow. This time, we specify the `type` as `:function`. This indicates that this entry is a function that takes a segment and returns either a segment or a seq of segments. Simple as that. Its concrete function is bound through `:onyx/fn` to a fully qualified namespace. Make sure the file containing this function is required onto the classpath before it's executed!

##### `loud-output`

```clojure
{:onyx/name :loud-output
 :onyx/tenancy-ident :core.async/write-to-chan
 :onyx/type :output
 :onyx/medium :core.async
 :onyx/max-peers 1
 :onyx/batch-size batch-size
 :onyx/doc "Writes segments to a core.async channel"}
```

Here again, we use the core.async plugin as a convenience for output. The `name` corresponds to the `:loud-output` seen in the workflow.

##### Full catalog

For reference, here's the full catalog:

```clojure
(def catalog
  [{:onyx/name :in
    :onyx/tenancy-ident :core.async/read-from-chan
    :onyx/type :input
    :onyx/medium :core.async
    :onyx/max-peers 1
    :onyx/batch-size batch-size
    :onyx/doc "Reads segments from a core.async channel"}

   {:onyx/name :split-by-spaces
    :onyx/fn :onyx-starter.core/split-by-spaces
    :onyx/type :function
    :onyx/batch-size batch-size}

   {:onyx/name :mixed-case
    :onyx/fn :onyx-starter.core/mixed-case
    :onyx/type :function
    :onyx/batch-size batch-size}

   {:onyx/name :loud
    :onyx/fn :onyx-starter.core/loud
    :onyx/type :function
    :onyx/batch-size batch-size}

   {:onyx/name :question
    :onyx/fn :onyx-starter.core/question
    :onyx/type :function
    :onyx/batch-size batch-size}

   {:onyx/name :loud-output
    :onyx/tenancy-ident :core.async/write-to-chan
    :onyx/type :output
    :onyx/medium :core.async
    :onyx/max-peers 1
    :onyx/batch-size batch-size
    :onyx/doc "Writes segments to a core.async channel"}

   {:onyx/name :question-output
    :onyx/tenancy-ident :core.async/write-to-chan
    :onyx/type :output
    :onyx/medium :core.async
    :onyx/max-peers 1
    :onyx/batch-size batch-size
    :onyx/doc "Writes segments to a core.async channel"}])
```

#### Function Implementation

At some point, we need to actually specify what the functions that we named *are*. Now's as good a time as any. Notice that all the functions take segments as parameters, and emit one or more segments as output. I split out the destructuring logic from the application logic for clarity:

```clojure
(defn split-by-spaces-impl [s]
  (clojure.string/split s #"\s+"))

(defn mixed-case-impl [s]
  (->> (cycle [(memfn toUpperCase) (memfn toLowerCase)])
       (map #(%2 (str %1)) s)
       (apply str)))

(defn loud-impl [s]
  (str s "!"))

(defn question-impl [s]
  (str s "?"))

(defn split-by-spaces [segment]
  (map (fn [word] {:word word}) (split-by-spaces-impl (:sentence segment))))

(defn mixed-case [segment]
  {:word (mixed-case-impl (:word segment))})

(defn loud [segment]
  {:word (loud-impl (:word segment))})

(defn question [segment]
  {:word (question-impl (:word segment))})
```

#### State Management

You'll notice above that I mentioned we're using core.async for both input and output. One of the things we need to do is get a *handle* to the core.async channels to put data into them and get data out of them. Onyx ships with a Task Lifecycle API, allowing you to start and stop stateful entities used in your program. Let's add those channels in:

```clojure
(def input-chan (chan capacity))

(def loud-output-chan (chan capacity))

(def question-output-chan (chan capacity))

(defn inject-input-ch [event lifecycle]
  {:core.async/chan input-chan})

(defn inject-loud-output-ch [event lifecycle]
  {:core.async/chan loud-output-chan})

(defn inject-question-output-ch [event lifecycle]
  {:core.async/chan question-output-chan})

(def input-calls
  {:lifecycle/before-task-start inject-input-ch})

(def loud-output-calls
  {:lifecycle/before-task-start inject-loud-output-ch})

(def question-output-calls
  {:lifecycle/before-task-start inject-question-output-ch})

(defn build-lifecycles []
  [{:lifecycle/task :in
    :core.async/id (java.util.UUID/randomUUID)
    :lifecycle/calls :onyx-starter.lifecycles.sample-lifecycle/in-calls}
   {:lifecycle/task :in
    :lifecycle/calls :onyx.plugin.core-async/reader-calls}
   {:lifecycle/task :loud-output
    :lifecycle/calls :onyx-starter.lifecycles.sample-lifecycle/out-calls
    :core.async/id (java.util.UUID/randomUUID)
    :lifecycle/doc "Lifecycle for writing to a core.async chan"}
   {:lifecycle/task :loud-output
    :lifecycle/calls :onyx.plugin.core-async/writer-calls}
   {:lifecycle/task :question-output
    :lifecycle/calls :onyx-starter.lifecycles.sample-lifecycle/out-calls
    :core.async/id (java.util.UUID/randomUUID)
    :lifecycle/doc "Lifecycle for writing to a core.async chan"}
   {:lifecycle/task :question-output
    :lifecycle/calls :onyx.plugin.core-async/writer-calls}])
```

#### Submit the Job

Now that all of the above is defined, we can start up the Peers to execute the job:

```clojure
(let [dev-cfg (-> "dev-peer-config.edn" resource slurp read-string)
      peer-config (assoc dev-cfg :onyx/tenancy-id (:onyx-id dev-env))
      dev-catalog (build-catalog 10) 
      dev-lifecycles (build-lifecycles)]
  ;; Automatically pipes the data structure into the channel, attaching :done at the end
  (sl/bind-inputs! dev-lifecycles {:in dev-inputs/input-segments})
  (let [job {:workflow workflow
             :catalog dev-catalog
             :lifecycles dev-lifecycles
             :flow-conditions sf/flow-conditions
             :task-scheduler :onyx.task-scheduler/balanced}]
    (onyx.api/submit-job peer-config job)
    ;; Automatically grab output from the stubbed core.async channels,
    ;; returning a vector of the results with data structures representing
    ;; the output.
    (sl/collect-outputs! dev-lifecycles [:loud-output :question-output])))
```

We use some helper functions in the test to bind the inputs and collect the outputs:

```clojure
(deftest test-sample-dev-job
  ;; 8 peers for 8 distinct tasks in the workflow
  (let [dev-env (component/start (onyx-dev-env 8))]
    (try 
      (let [[loud-out question-out] (submit-sample/submit-job dev-env)]
        (clojure.pprint/pprint loud-out)
        (println)
        (clojure.pprint/pprint question-out)
        (is (= 12 (count question-out)))
        (is (= 12 (count loud-out))))
      (finally 
        (component/stop dev-env)))))
```

And the output is:

```clojure
[{:word "HeY!"}
 {:word "ThErE!"}
 {:word "UsEr!"}
 {:word "It's!"}
 {:word "ReAlLy!"}
 {:word "NiCe!"}
 {:word "OuTsIdE!"}
 {:word "I!"}
 {:word "LiVe!"}
 {:word "In!"}
 {:word "ReDmOnD!"}
 :done]

[{:word "HeY?"}
 {:word "ThErE?"}
 {:word "UsEr?"}
 {:word "It's?"}
 {:word "ReAlLy?"}
 {:word "NiCe?"}
 {:word "OuTsIdE?"}
 {:word "I?"}
 {:word "LiVe?"}
 {:word "In?"}
 {:word "ReDmOnD?"}
 :done]
```

A few notes:
- The last piece of input we sent through is not a segment, but `:done`! It's called the Sentinel. This is a specially recognized value in Onyx which *completes* the current running task. This value is used to switch transparently between batch and streaming modes.
- The `id` on the Peers in a distributed environment *must* match up for them to work together. This is how they find each other when there are multiple deployments.
- We start **8** virtual peers, which is a unit of local parallelism in Onyx. You need at least one virtual peer per task for your job to start.
- The sentinel is helpfully propagated downstream, so you know when you've got to the end of an output stream.

#### Conclusion

Hope this helped. Enjoy Onyx!
