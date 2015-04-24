### The Anatomy of an Onyx Program

In this tutorial, we'll take an in-depth view of what's happening when you execute a simple Onyx program. All of the code can be found in the [Onyx Starter repository](https://github.com/MichaelDrogalis/onyx-starter) if you'd like to follow along. The code uses the development environment with HornetQ and ZooKeeper running in memory, so you don't need additional dependencies to run the example for yourself on your machine.

#### The Workflow

At the core of the program is the workflow - the flow of data that we ingest, apply transformations to, and send to an output for storage. In this program, we're going to ingest some sentences from an input source, split the sentence into individual words, play with capitalization, and add a suffix. Finally, we'll send the transformed data to an output source.

Let's examine the workflow pictorially:

![Basic](http://i.imgur.com/uJEenZq.png)

Notice that when we get to the interleaving of case case, we *fork* the computation into two distinct streams. This demonstrates a fundamental ability of Onyx to model directed, acyclic graphs as trees. Let's add another level of detail and show some actual data passing through each stage:

![Detailed](http://i.imgur.com/VMueGgh.png)

A few things to note:
- This diagram shows data *as it's leaving* each transformation.
- Onyx only allows *maps* to be ingested and emitted. These are called *segments*.
- Onyx multiple segments to be emitted in a vector, as seen in the `split by spaces` transformation.
- After `split by spaces`, the diagram only shows `{:word "really"}` moving through the workflow. This is simply for saving space. All words move through the rest of the workflow in the same manner.

All of this is expressed in Clojure as the following form:

```clojure
(def workflow
  {:in
   {:split-by-spaces
    {:mixed-case
     {:loud :loud-output
      :question :question-output}}}})
```

Or, starting in `0.4.0`, you can express it as a directed, acylic graph:

```clojure
[[:in :split-by-spaces]
 [:split-by-spaces :mixed-case]
 [:mixed-case :loud]
 [:mixed-case :question]
 [:loud :loud-output]
 [:question :question-output]]
```

The latter style allows data paths to merge back together, whereas the former is more clear for strictly outward branching data flows.

#### The Catalog

Now that we've outlined the flow of data for our program in the workflow, let's bind the names we used in the workflow to entries in the catalog. The catalog is used to set up context for functions that will eventually execute on the cluster. We'll look at a few of the catalog entries next.

##### `in`

```clojure
{:onyx/name :in
 :onyx/ident :core.async/read-from-chan
 :onyx/type :input
 :onyx/medium :core.async
 :onyx/consumption :sequential
 :onyx/batch-size batch-size
 :onyx/doc "Reads segments from a core.async channel"}
```

The first entry in the catalog specifies the input for the workflow. Here, we make use of the core.async plugin for Onyx to make local development convenient. `ident`, `type`, and `medium` are specified by the plugin and should be copied directly. `consumption` and `batch-size` are performance tuning parameters. `name` corresponds to `:in` in the workflow that we specified above.

##### `split-by-spaces`

```clojure
{:onyx/name :split-by-spaces
 :onyx/fn :onyx-starter.core/split-by-spaces
 :onyx/type :function
 :onyx/consumption :concurrent
 :onyx/batch-size batch-size}
```

Now we turn our attention to the `split-by-spaces` function. Again, the name corresponds to that which is used in the workflow. This time, we specify the `type` as `:function`. This indicates that this entry is a function that takes a segment and returns either a segment or a seq of segments. Simple as that. It's concrete function is bound through `:onyx/fn` to a fully qualified namespace. Make sure the file containing this function is required onto the classpath before its executed!

##### `loud-output`

```clojure
{:onyx/name :loud-output
 :onyx/ident :core.async/write-to-chan
 :onyx/type :output
 :onyx/medium :core.async
 :onyx/consumption :sequential
 :onyx/batch-size batch-size
 :onyx/doc "Writes segments to a core.async channel"}
```

Here again, we use the core.async plugin as a convenience for output. The `name` corresponds to the `:loud-output` seen in the workflow.

##### Full catalog

For reference, here's the full catalog:

```clojure
(def catalog
  [{:onyx/name :in
    :onyx/ident :core.async/read-from-chan
    :onyx/type :input
    :onyx/medium :core.async
    :onyx/consumption :sequential
    :onyx/batch-size batch-size
    :onyx/doc "Reads segments from a core.async channel"}

   {:onyx/name :split-by-spaces
    :onyx/fn :onyx-starter.core/split-by-spaces
    :onyx/type :function
    :onyx/consumption :concurrent
    :onyx/batch-size batch-size}

   {:onyx/name :mixed-case
    :onyx/fn :onyx-starter.core/mixed-case
    :onyx/type :function
    :onyx/consumption :concurrent
    :onyx/batch-size batch-size}

   {:onyx/name :loud
    :onyx/fn :onyx-starter.core/loud
    :onyx/type :function
    :onyx/consumption :concurrent
    :onyx/batch-size batch-size}

   {:onyx/name :question
    :onyx/fn :onyx-starter.core/question
    :onyx/type :function
    :onyx/consumption :concurrent
    :onyx/batch-size batch-size}

   {:onyx/name :loud-output
    :onyx/ident :core.async/write-to-chan
    :onyx/type :output
    :onyx/medium :core.async
    :onyx/consumption :sequential
    :onyx/batch-size batch-size
    :onyx/doc "Writes segments to a core.async channel"}

   {:onyx/name :question-output
    :onyx/ident :core.async/write-to-chan
    :onyx/type :output
    :onyx/medium :core.async
    :onyx/consumption :sequential
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
(ns onyx-starter.core
  (:require [clojure.core.async :refer [chan >!! <!! close!]]
            [onyx.peer.task-lifecycle-extensions :as l-ext]
            [onyx.plugin.core-async]
            [onyx.api]))

(def input-chan (chan capacity))

(def loud-output-chan (chan capacity))

(def question-output-chan (chan capacity))

(defmethod l-ext/inject-lifecycle-resources :input
  [_ _] {:core-async/in-chan input-chan})

(defmethod l-ext/inject-lifecycle-resources :loud-output
  [_ _] {:core-async/out-chan loud-output-chan})

(defmethod l-ext/inject-lifecycle-resources :question-output
  [_ _] {:core-async/out-chan question-output-chan})
```

The core.async plugin expects `:core-async/in-chan` defined with a channel for input, and `:core-async/out-chan` for output.

#### Submit the Job

Now that all of the above is defined, we can start up the Peers to execute the job:

```clojure
(def input-segments
  [{:sentence "Hey there user"}
   {:sentence "It's really nice outside"}
   {:sentence "I live in Redmond"}
   :done])

;;; Put the data onto the input chan       
(doseq [segment input-segments]
  (>!! input-chan segment))

(close! input-chan)

(def id (java.util.UUID/randomUUID))

;; Use the Round Robin job scheduler                                                                                                                        
(def scheduler :onyx.job-scheduler/round-robin)

(def env-config
  {:hornetq/mode :vm
   :hornetq.server/type :vm
   :hornetq/server? true
   :zookeeper/address "127.0.0.1:2186"
   :zookeeper/server? true
   :zookeeper.server/port 2186
   :onyx/id id
   :onyx.peer/job-scheduler scheduler})

(def peer-config
  {:hornetq/mode :vm
   :zookeeper/address "127.0.0.1:2186"
   :onyx/id id
   :onyx.peer/job-scheduler scheduler})

;; Start an in-memory ZooKeeper and HornetQ                                                                                                                 
(def env (onyx.api/start-env env-config))

;; Start the worker peers.                                                                                                                                  
(def v-peers (onyx.api/start-peers! 1 peer-config))

(onyx.api/submit-job peer-config
                     {:catalog catalog :workflow workflow
                      :task-scheduler :onyx.task-scheduler/round-robin})

(def loud-results (onyx.plugin.core-async/take-segments! loud-output-chan))

(def question-results (onyx.plugin.core-async/take-segments! question-output-chan))

(clojure.pprint/pprint loud-results)

(println)

(clojure.pprint/pprint question-results)

(doseq [v-peer v-peers]
  (onyx.api/shutdown-peer v-peer))

(onyx.api/shutdown-env env)
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
- We start **1** virtual peer, which is a unit of local parallelism in Onyx. One virtual peer is usually fine for development. One virtual peer executes one task, so if you're running a streaming job, you need at least n virtual peers for n running tasks.
- The sentinel is helpfully propagated downstream, so you know when you've got to the end of an output stream.

#### Conclusion

Hope this helped. Enjoy Onyx!