(ns prime-sieve.core)

(defn primes []
  (letfn [(sieve-step [candidate composites]
            (if (contains? composites candidate)
              (let [prime (get composites candidate)
                    next-multiple (loop [n (+ candidate prime)]
                                    (if (contains? composites n)
                                      (recur (+ n prime))
                                      n))]
                (recur (inc candidate)
                       (-> composites
                           (dissoc candidate)
                           (assoc next-multiple prime))))
              (lazy-seq
                (cons candidate
                      (sieve-step (inc candidate)
                                  (assoc composites (* candidate candidate) candidate))))))]
    (sieve-step 2 {})))

(defn primes-up-to
  "Returns all prime numbers up to n (inclusive)."
  [n]
  (take-while #(<= % n) (primes)))

(defn first-n-primes
  "Returns the first n prime numbers."
  [n]
  (take n (primes)))

(defn nth-prime
  "Returns the nth prime number (0-indexed)."
  [n]
  (nth (primes) n))

(defn is-prime?
  "Checks if a number is prime using trial division.
  More efficient than searching through the prime sequence."
  [n]
  (cond
    (< n 2) false
    (= n 2) true
    (even? n) false
    :else (let [limit (inc (int (Math/sqrt n)))]
            (not-any? #(zero? (mod n %)) (range 3 limit 2)))))

(defn -main
  "Demonstrates the infinite prime sequence."
  [& args]
  (println "First 20 prime numbers:")
  (println (first-n-primes 20))
  (println "\nPrimes up to 100:")
  (println (primes-up-to 100))
  (println "\nThe 100th prime number:")
  (println (nth-prime 99))
  (println "\nIs 97 prime?")
  (println (is-prime? 97)))