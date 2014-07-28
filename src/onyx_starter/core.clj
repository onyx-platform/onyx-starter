(ns onyx-starter.core
  (:require [clojure.core.async :refer [chan >!! <!! close!]]
            [onyx.peer.task-lifecycle-extensions :as l-ext]
            [onyx.plugin.core-async]
            [onyx.api]))

;;;;; Implementation functions ;;;;;
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

;;;;; Destructuring functions ;;;;;
(defn split-by-spaces [segment]
  (map (fn [word] {:word word}) (split-by-spaces-impl (:sentence segment))))

(defn mixed-case [segment]
  {:word (mixed-case-impl (:word segment))})

(defn loud [segment]
  {:word (loud-impl (:word segment))})

(defn question [segment]
  {:word (question-impl (:word segment))})

;;;;; Configuration ;;;;;

;;;            input
;;;              |
;;;       split-by-spaces
;;;              |
;;;          mixed-case
;;;            /    \
;;;         loud     question
;;;           |         |
;;;    loud-output    question-output

(def workflow
  {:input
   {:split-by-spaces
    {:mixed-case
     {:loud :loud-output
      :question :question-output}}}})

(def capacity 1000)

(def input-chan (chan capacity))

(def loud-output-chan (chan capacity))

(def question-output-chan (chan capacity))

;;; Inject the channels needed by the core.async plugin for each
;;; input and output.
(defmethod l-ext/inject-lifecycle-resources :input
  [_ _] {:core-async/in-chan input-chan})

(defmethod l-ext/inject-lifecycle-resources :loud-output
  [_ _] {:core-async/out-chan loud-output-chan})

(defmethod l-ext/inject-lifecycle-resources :question-output
  [_ _] {:core-async/out-chan question-output-chan})

(def batch-size 10)

(def catalog
  [{:onyx/name :input
    :onyx/ident :core.async/read-from-chan
    :onyx/type :input
    :onyx/medium :core.async
    :onyx/consumption :concurrent
    :onyx/batch-size batch-size
    :onyx/doc "Reads segments from a core.async channel"}

   {:onyx/name :split-by-spaces
    :onyx/fn :onyx-starter.core/split-by-spaces
    :onyx/type :transformer
    :onyx/consumption :concurrent
    :onyx/batch-size batch-size}

   {:onyx/name :mixed-case
    :onyx/fn :onyx-starter.core/mixed-case
    :onyx/type :transformer
    :onyx/consumption :concurrent
    :onyx/batch-size batch-size}

   {:onyx/name :loud
    :onyx/fn :onyx-starter.core/loud
    :onyx/type :transformer
    :onyx/consumption :concurrent
    :onyx/batch-size batch-size}

   {:onyx/name :question
    :onyx/fn :onyx-starter.core/question
    :onyx/type :transformer
    :onyx/consumption :concurrent
    :onyx/batch-size batch-size}

   {:onyx/name :loud-output
    :onyx/ident :core.async/write-to-chan
    :onyx/type :output
    :onyx/medium :core.async
    :onyx/consumption :concurrent
    :onyx/batch-size batch-size
    :onyx/doc "Writes segments to a core.async channel"}

   {:onyx/name :question-output
    :onyx/ident :core.async/write-to-chan
    :onyx/type :output
    :onyx/medium :core.async
    :onyx/consumption :concurrent
    :onyx/batch-size batch-size
    :onyx/doc "Writes segments to a core.async channel"}])

;;; Input data to pipe into the input channel, plus the
;;; sentinel to signal the end of input.
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

(def coord-opts
  {:hornetq/mode :vm ;; Run HornetQ inside the VM for convenience
   :hornetq/server? true
   :hornetq.server/type :vm
   :zookeeper/address "127.0.0.1:2185"
   :zookeeper/server? true ;; Run ZK inside the VM for convenience
   :zookeeper.server/port 2185
   :onyx/id id
   :onyx.coordinator/revoke-delay 5000})

(def peer-opts
  {:hornetq/mode :vm
   :zookeeper/address "127.0.0.1:2185"
   :onyx/id id})

;; Connect to the coordinator. Also boots up in the in-memory services.
(def conn (onyx.api/connect :memory coord-opts))

;; Start the worker peers.
(def v-peers (onyx.api/start-peers conn 1 peer-opts))

(onyx.api/submit-job conn {:catalog catalog :workflow workflow})

;; Iterate 11 times, since there's 11 words in the 3 sentences above.
(def loud-results (doall (map (fn [_] (<!! loud-output-chan)) (range 11))))

(def question-results (doall (map (fn [_] (<!! question-output-chan)) (range 11))))

(clojure.pprint/pprint loud-results)

(println)

(clojure.pprint/pprint question-results)

(doseq [v-peer v-peers]
  ((:shutdown-fn v-peer)))

(onyx.api/shutdown conn)

(defn -main [& args]
  ;; Invoking main executes the above, so we're instantly done by time
  ;; we get here.
  (println "Done!"))

