(ns task-c1.core
  (:gen-class))

(defn strings-without-adjacent [alphabet n]
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
  (println (strings-without-adjacent ["a" "b" "c"] 2))
  (println "\nTest 2:")
  (println (strings-without-adjacent ["a" "b"] 3)))
