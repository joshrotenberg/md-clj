(defproject md-clj "0.0.1"
  :description "0MQ Majordomo Protocol Wrapper"
  :dependencies [[clojure "1.3.0"]
                 [jzmq "2.1.0"]]
  :dev-dependencies [[lein-marginalia "0.6.1"]
                     [re-rand "0.1.0"]
                     [compojure "0.6.5"]
                     [ring/ring-jetty-adapter "0.3.9"]
                     [clj-http "0.2.6"]
                     [cheshire "2.0.4"]
                     [lein-clojars "0.6.0"]]
  :source-path "src/clj"
  :java-source-path "src/jvm"
  :jvm-opts ["-Djava.library.path=/usr/local/lib:/opt/local/lib:/usr/lib"])
