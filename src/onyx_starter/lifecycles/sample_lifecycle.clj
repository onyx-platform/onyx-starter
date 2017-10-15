(ns onyx-starter.lifecycles.sample-lifecycle
  (:require [clojure.core.async :refer [chan sliding-buffer >!!]]
            [onyx.plugin.core-async :refer [take-segments!]]
            [onyx.static.planning :refer [find-task]]))

;;; Lifecycles are hooks to add in extra code between predefined
;;; points in the execution of a task on a peer.

(def input-channel-capacity 10000)

(def output-channel-capacity (inc input-channel-capacity))

;;; A pair of functions to get channels from a memoized reference.
;;; We memoize it by requested ID because we want to get a reference
;;; to read and write segments, and also so that peers can locate a reference
;;; when they use the channel in the job.

(def get-input-channel
  (memoize
   (fn [id]
     (chan input-channel-capacity))))

(def get-output-channel
  (memoize
   (fn [id]
     (chan (sliding-buffer output-channel-capacity)))))

(defn channel-id-for [lifecycles task-name]
  (:core.async/id
   (->> lifecycles
        (filter #(= task-name (:lifecycle/task %)))
        (first))))

(defn bind-inputs! [lifecycles mapping]
  (doseq [[task segments] mapping]
    (let [in-ch (get-input-channel (channel-id-for lifecycles task))]
      (doseq [segment segments]
        (>!! in-ch segment)))))

(defn collect-outputs! [lifecycles output-tasks]
  (->> output-tasks
       (map #(get-output-channel (channel-id-for lifecycles %)))
       (map #(take-segments! % 5000))
       (zipmap output-tasks)))

(defn inject-in-ch [event lifecycle]
  {:core.async/buffer (atom {})
   :core.async/chan (get-input-channel (:core.async/id lifecycle))})

(defn inject-out-ch [event lifecycle]
  {:core.async/chan (get-output-channel (:core.async/id lifecycle))})

(def in-calls
  {:lifecycle/before-task-start inject-in-ch})

(def out-calls
  {:lifecycle/before-task-start inject-out-ch})

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
