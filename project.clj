(defproject onyx-starter "0.8.4"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/core.async "0.2.371"]
                 ^{:voom {:repo "git@github.com:onyx-platform/onyx.git" :branch "master"}}
                 [org.onyxplatform/onyx "0.8.5-20160114_164623-gc81b6ea"]
                 [com.stuartsierra/component "0.2.3"]]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.10"]]
                   :plugins [[lein-update-dependency "0.1.2"]
                             [lein-set-version "0.4.1"]]
                   :source-paths ["env/dev" "src"]}})
