(defproject onyx-starter "0.12.7"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.onyxplatform/onyx "0.12.7"]
                 [com.stuartsierra/component "0.3.2"]]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.10"]]
                   :plugins [[lein-update-dependency "0.1.2"]
                             [lein-pprint "1.2.0"]
                             [lein-set-version "0.4.1"]]
                   :source-paths ["env/dev" "src"]}})
