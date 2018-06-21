(ns onyx-starter.jobs.sample-job-test
  (:require [clojure.test :refer [deftest is]]
            [com.stuartsierra.component :as component]
            [onyx-starter.launcher.submit-sample-job :as submit-sample]
            [onyx-starter.workflows.sample-workflow :as workflows]
            [onyx-starter.launcher.dev-system :refer [onyx-dev-env]]
            [onyx-starter.functions.sample-functions]
            [onyx.api]))

(defn count-distinct-tasks [workflow]
  (->> workflow
       flatten
       set
       count))

(deftest test-sample-dev-job
  (let [distinct-workflows (count-distinct-tasks workflows/workflow)
        dev-env            (component/start (onyx-dev-env distinct-workflows))]
    (try
      (let [{:keys [loud-output question-output period-output]} (submit-sample/submit-job dev-env)]
        (clojure.pprint/pprint loud-output)
        (println)
        (clojure.pprint/pprint question-output)
        (println)
        (clojure.pprint/pprint period-output)
        (is (= 11 (count question-output)))
        (is (= 11 (count loud-output)))
        (is (= 11 (count period-output)))
        )
      (finally
        (component/stop dev-env)))))
