(defproject cuic "0.1.4"
  :description "Concise UI testing with Clojure"
  :url "https://github.com/milankinen/cuic"
  :license {:name "MIT"
            :url  "https://opensource.org/licenses/MIT"}
  :signing {:gpg-key "9DD8C3E9"}
  :repositories [["JCenter" "https://jcenter.bintray.com/"]]
  :dependencies [[org.clojure/tools.logging "0.4.1"]
                 [org.clojure/data.json "0.2.6"]
                 [com.github.kklisura.cdt/cdt-java-client "1.3.1"]
                 [org.jsoup/jsoup "1.11.3"]
                 [com.github.kilianB/JImageHash "1.0.2"]]
  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]
  :resource-paths ["resources"]
  :jar-exclusions [#"js_deps/"]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.9.0"]
                                  [http-kit "2.3.0"]
                                  [compojure "1.6.1"]
                                  [ch.qos.logback/logback-classic "1.2.3"]
                                  [eftest "0.5.3"]]
                   :plugins      [[lein-ancient "0.6.15"]]}}
  :deploy-repositories [["releases" :clojars]]
  :aliases {"test"    ["trampoline" "run" "-m" "test-runner/run-tests-cli"]
            "t"       ["test"]
            "release" ["do" ["clean"] ["deploy" "clojars"]]}
  :release-tasks [["deploy"]])
