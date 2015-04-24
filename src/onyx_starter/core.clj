(ns onyx-starter.core
  (:require [clojure.core.async :refer [chan >!! <!! close!]]
            [onyx.peer.task-lifecycle-extensions :as l-ext]
            [onyx.plugin.core-async :refer [take-segments!]]
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

;;;             in
;;;              |
;;;       split-by-spaces
;;;              |
;;;          mixed-case
;;;            /    \
;;;         loud     question
;;;           |         |
;;;    loud-output    question-output

(def workflow
  [[:in :split-by-spaces]
   [:split-by-spaces :mixed-case]
   [:mixed-case :loud]
   [:mixed-case :question]
   [:loud :loud-output]
   [:question :question-output]])

(def capacity 1000)

(def input-chan (chan capacity))

(def loud-output-chan (chan capacity))

(def question-output-chan (chan capacity))

;;; Inject the channels needed by the core.async plugin for each
;;; input and output.
(defmethod l-ext/inject-lifecycle-resources :in
  [_ _] {:core.async/chan input-chan})

(defmethod l-ext/inject-lifecycle-resources :loud-output
  [_ _] {:core.async/chan loud-output-chan})

(defmethod l-ext/inject-lifecycle-resources :question-output
  [_ _] {:core.async/chan question-output-chan})

(def batch-size 10)

(def catalog
  [{:onyx/name :in
    :onyx/ident :core.async/read-from-chan
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
    :onyx/ident :core.async/write-to-chan
    :onyx/type :output
    :onyx/medium :core.async
    :onyx/max-peers 1
    :onyx/batch-size batch-size
    :onyx/doc "Writes segments to a core.async channel"}

   {:onyx/name :question-output
    :onyx/ident :core.async/write-to-chan
    :onyx/type :output
    :onyx/medium :core.async
    :onyx/max-peers 1
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

(def env-config
  {:zookeeper/address "127.0.0.1:2188"
   :zookeeper/server? true
   :zookeeper.server/port 2188
   :onyx/id id})

(def peer-config
  {:zookeeper/address "127.0.0.1:2188"
   :onyx/id id
   :onyx.peer/job-scheduler :onyx.job-scheduler/balanced
   :onyx.messaging/impl :core.async
   :onyx.messaging/bind-addr "localhost"})

;; Start an in-memory ZooKeeper and HornetQ (depending on vm-hornetq?)
(def env (onyx.api/start-env env-config))

;; Start a peer group to share resources.
(def peer-group (onyx.api/start-peer-group peer-config))

;; We need at least one peer per task.
(def n-peers (count (set (mapcat identity workflow))))

;; Start the worker peers.
(def v-peers (onyx.api/start-peers n-peers peer-group))

(onyx.api/submit-job peer-config
                     {:catalog catalog :workflow workflow
                      :task-scheduler :onyx.task-scheduler/balanced})

(def loud-results (onyx.plugin.core-async/take-segments! loud-output-chan))

(def question-results (onyx.plugin.core-async/take-segments! question-output-chan))

(clojure.pprint/pprint loud-results)

(println)

(clojure.pprint/pprint question-results)

(doseq [v-peer v-peers]
  (onyx.api/shutdown-peer v-peer))

(onyx.api/shutdown-peer-group peer-group)

(onyx.api/shutdown-env env)
