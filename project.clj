(defproject onyx-starter "0.9.10-beta4"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/core.async "0.2.371"]
                 [org.onyxplatform/onyx "0.9.10-20160911_142440-g2f36637"]
                 [com.stuartsierra/component "0.2.3"]]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.10"]]
                   :plugins [[lein-update-dependency "0.1.2"]
                             [lein-pprint "1.1.1"]
                             [lein-set-version "0.4.1"]]
                   :source-paths ["env/dev" "src"]}})
