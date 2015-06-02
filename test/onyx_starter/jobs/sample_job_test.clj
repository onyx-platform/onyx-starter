(ns onyx-starter.jobs.sample-job-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.core.async :refer [>!!]]
            [clojure.java.io :refer [resource]]
            [com.stuartsierra.component :as component]
            [onyx-starter.launcher.dev-system :refer [onyx-dev-env]]
            [onyx-starter.workflows.sample-workflow :refer [workflow]]
            [onyx-starter.catalogs.sample-catalog :refer [build-catalog] :as sc]
            [onyx-starter.lifecycles.sample-lifecycle :refer [build-lifecycles] :as sl]
            [onyx-starter.flow-conditions.sample-flow-conditions :as sf]
            [onyx-starter.functions.sample-functions]
            [onyx-starter.dev-inputs.sample-input :as dev-inputs]
            [onyx-starter.launcher.submit-sample-job :as submit-sample]
            [onyx.api]))

(deftest test-sample-dev-job
  (let [dev-env (component/start (onyx-dev-env 8))]
    (try 
      (let [[loud-out question-out] (submit-sample/submit-job dev-env)]
        (is (= 12 (count question-out)))
        (is (= 12 (count loud-out))))
      (finally 
        (component/stop dev-env)))))
