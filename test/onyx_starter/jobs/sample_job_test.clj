(ns onyx-starter.jobs.sample-job-test
  (:require [clojure.test :refer [deftest is]]
            [com.stuartsierra.component :as component]
            [onyx-starter.launcher.submit-sample-job :as submit-sample]
            [onyx-starter.launcher.dev-system :refer [onyx-dev-env]]
            [onyx-starter.functions.sample-functions]
            [onyx.api]))

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
