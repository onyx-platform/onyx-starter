(ns onyx-starter.launcher.submit-sample-job
  (:require [com.stuartsierra.component :as component]
            [clojure.java.io :refer [resource]]
            [onyx-starter.launcher.dev-system :refer [onyx-dev-env]]
            [onyx-starter.workflows.sample-workflow :refer [workflow]]
            [onyx-starter.catalogs.sample-catalog :refer [build-catalog] :as sc]
            [onyx-starter.lifecycles.sample-lifecycle :refer [build-lifecycles] :as sl]
            [onyx-starter.flow-conditions.sample-flow-conditions :as sf]
            [onyx-starter.functions.sample-functions]
            [onyx-starter.dev-inputs.sample-input :as dev-inputs]
            [onyx.api]))

(defn submit-job [dev-env] 
  (let [dev-cfg (-> "dev-peer-config.edn" resource slurp read-string)
        peer-config (assoc dev-cfg :onyx/id (:onyx-id dev-env))
        dev-catalog (build-catalog 10 50) 
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
      (sl/collect-outputs! dev-lifecycles [:loud-output :question-output]))))
