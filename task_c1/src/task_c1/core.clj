(ns task-c1.core
  (:gen-class))

(defn builder [alphabet n]
  (if (= n 0)
    [""]
    (reduce
     (fn [current-strings _]
       (apply concat
              (map
               (fn [s]
                 (let [last-char (str (last s))]
                   (map #(str s %)
                        (remove #(= % last-char) alphabet))))
               current-strings)))
     (map str alphabet)
     (range 1 n))))

(defn -main []
  (println "Test 1:")
  (println (builder ["a" "b" "c"] 2))
  (println "\nTest 2:")
  (println (builder ["a" "b"] 3)))
