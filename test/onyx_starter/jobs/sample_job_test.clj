(ns onyx-starter.jobs.sample-job-test
  (:require [clojure.test :refer [deftest is]]
            [com.stuartsierra.component :as component]
            [onyx-starter.launcher.submit-sample-job :as submit-sample]
            [onyx-starter.launcher.dev-system :refer [onyx-dev-env]]
            [onyx-starter.functions.sample-functions]
            [onyx.api]))

(deftest test-sample-dev-job
  ;; 7 peers for 7 distinct tasks in the workflow
  (let [dev-env (component/start (onyx-dev-env 7))]
    (try 
      (let [{:keys [loud-output question-output]} (submit-sample/submit-job dev-env)]
        (clojure.pprint/pprint loud-output)
        (println)
        (clojure.pprint/pprint question-output)
        (is (= 11 (count question-output)))
        (is (= 11 (count loud-output))))
      (finally 
        (component/stop dev-env)))))
