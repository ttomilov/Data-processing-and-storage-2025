(ns task-c3.core
  (:gen-class))

(defn parallel-filter
  "Ленивый параллельный filter по блокам.
   pred        — предикат
   block-size  — размер блока, который обрабатывает один future (>= 1)
   coll        — входная последовательность (может быть бесконечной)

   Для ограничения числа одновременно работающих futures используется
   минимальное значение между `block-size` и количеством доступных ядер."
  [pred block-size coll]
  (let [block-size (long block-size)]
    (when (<= block-size 0)
      (throw (ex-info "block-size must be positive" {:block-size block-size})))

    (let [;; Количество параллельных задач не превышает числа ядер
          ;; и не может быть больше размера блока, чтобы не создавать
          ;; лишние futures без работы.
          workers (max 1 (min block-size (.availableProcessors (Runtime/getRuntime))))
          blocks  (partition-all block-size coll)]

      (letfn [(step [remaining]
                (lazy-seq
                  (when-let [s (seq remaining)]
                    (let [[batch rest-blocks] (split-at workers s)
                          futures (mapv #(future (doall (filter pred %))) batch)]
                      (concat (mapcat deref futures)
                              (step rest-blocks))))))]

        (step blocks)))))

(defn- parse-long*
  "Parses string to long with fallback." 
  [s default]
  (try
    (Long/parseLong s)
    (catch Exception _ default)))

(defn -main
  "Runs a small demo comparing sequential vs parallel filter.
   Args: [n block-size], defaults n=400, block-size=32."
  [& args]
  (let [[n block-size] (map #(or % "") args)
      n (parse-long* n 400)
      block-size (parse-long* block-size 32)
        data (range n)
      slow-pred (fn [x]
            (Thread/sleep 5)
            (even? x))
      measure (fn [f]
            (let [start (System/nanoTime)
              res (f)
              ms (/ (- (System/nanoTime) start) 1e6)]
            [res ms]))]
    (println "Elements:" n "Block size:" block-size)
    (flush)
    (let [[_ seq-ms] (measure #(count (filter slow-pred data)))
        [_ par-ms] (measure #(count (parallel-filter slow-pred block-size data)))]
      (println "Sequential ms:" (format "%.1f" seq-ms)
           "Parallel ms:" (format "%.1f" par-ms))
      (flush))))