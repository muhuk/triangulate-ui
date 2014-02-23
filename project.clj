(defproject triangulate-ui "0.1.0-SNAPSHOT"
  :description "GUI for triangulate."
  :url "https://github.com/muhuk/triangulate-ui"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [triangulate "0.1.0"]]
  :plugins  [[lein-cloverage "1.0.2"]
             [lein-midje "3.1.3"]]
  :profiles {:dev {:dependencies [[midje "1.6.0"]]}}
  :main triangulate-ui.core)
