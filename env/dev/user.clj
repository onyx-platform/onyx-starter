(ns user
  (:require [clojure.tools.namespace.repl :refer [refresh]]
            [com.stuartsierra.component :as component]
            [onyx-starter.launcher.dev-system :refer [onyx-dev-env]]))

(def n-peers 10)

(def system nil)

(defn init []
  (alter-var-root #'system (constantly (onyx-dev-env n-peers))))

(defn start []
  (alter-var-root #'system component/start))

(defn stop []
  (alter-var-root #'system (fn [s] (when s (component/stop s)))))

(defn go []
  (init)
  (start))

(defn reset []
  (stop)
  (refresh :after 'user/go))
