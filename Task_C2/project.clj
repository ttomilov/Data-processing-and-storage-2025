(defproject prime-sieve "0.1.0-SNAPSHOT"
  :description "Infinite Sieve of Eratosthenes implementation"
  :dependencies [[org.clojure/clojure "1.11.1"]]
  :main ^:skip-aot prime-sieve.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
