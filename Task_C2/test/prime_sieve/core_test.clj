(ns prime-sieve.core-test
  (:require [clojure.test :refer :all]
            [prime-sieve.core :refer :all]))

(deftest test-first-n-primes
  (testing "First few prime numbers"
    (is (= [2 3 5 7 11] (first-n-primes 5)))
    (is (= [2] (first-n-primes 1)))
    (is (= [] (first-n-primes 0))))
  
  (testing "First 10 primes"
    (is (= [2 3 5 7 11 13 17 19 23 29] (first-n-primes 10))))
  
  (testing "First 25 primes"
    (is (= [2 3 5 7 11 13 17 19 23 29 31 37 41 43 47 53 59 61 67 71 73 79 83 89 97]
           (first-n-primes 25)))))

(deftest test-primes-up-to
  (testing "Primes up to small numbers"
    (is (= [2] (primes-up-to 2)))
    (is (= [2 3] (primes-up-to 3)))
    (is (= [2 3 5] (primes-up-to 5)))
    (is (= [2 3 5 7] (primes-up-to 10))))
  
  (testing "Primes up to 30"
    (is (= [2 3 5 7 11 13 17 19 23 29] (primes-up-to 30))))
  
  (testing "Primes up to 50"
    (is (= [2 3 5 7 11 13 17 19 23 29 31 37 41 43 47] (primes-up-to 50)))))

(deftest test-nth-prime
  (testing "Specific prime positions"
    (is (= 2 (nth-prime 0)))   ; 1st prime
    (is (= 3 (nth-prime 1)))   ; 2nd prime
    (is (= 5 (nth-prime 2)))   ; 3rd prime
    (is (= 7 (nth-prime 3)))   ; 4th prime
    (is (= 11 (nth-prime 4)))  ; 5th prime
    (is (= 29 (nth-prime 9)))  ; 10th prime
    (is (= 541 (nth-prime 99)))) ; 100th prime
  
  (testing "Larger prime positions"
    (is (= 7919 (nth-prime 999))))) ; 1000th prime

(deftest test-is-prime
  (testing "Small numbers"
    (is (false? (is-prime? 0)))
    (is (false? (is-prime? 1)))
    (is (true? (is-prime? 2)))
    (is (true? (is-prime? 3)))
    (is (false? (is-prime? 4)))
    (is (true? (is-prime? 5))))
  
  (testing "Composite numbers"
    (is (false? (is-prime? 6)))
    (is (false? (is-prime? 8)))
    (is (false? (is-prime? 9)))
    (is (false? (is-prime? 10)))
    (is (false? (is-prime? 15)))
    (is (false? (is-prime? 100))))
  
  (testing "Prime numbers"
    (is (true? (is-prime? 7)))
    (is (true? (is-prime? 11)))
    (is (true? (is-prime? 13)))
    (is (true? (is-prime? 17)))
    (is (true? (is-prime? 97)))))

(deftest test-infinite-sequence
  (testing "Sequence is lazy and can produce many primes"
    (let [large-primes (take 1000 (primes))]
      (is (= 1000 (count large-primes)))
      (is (= 2 (first large-primes)))
      (is (= 7919 (last large-primes)))))
  
  (testing "Sequence continues generating beyond first 1000"
    ; Test that we can generate more primes beyond the first batch
    (is (= 104729 (nth-prime 9999))))) ; 10,000th prime

(deftest test-sieve-algorithm
  (testing "Sieve correctly filters multiples"
    (let [primes-seq (primes)]
      ; Check that no prime is divisible by any smaller prime
      (doseq [p (take 50 primes-seq)]
        (is (true? (is-prime? p))))))
  
  (testing "All numbers in sequence are actually prime"
    (let [first-100 (first-n-primes 100)]
      (is (every? is-prime? first-100)))))

(deftest test-edge-cases
  (testing "Empty or invalid inputs"
    (is (= [] (first-n-primes 0)))
    (is (= [] (primes-up-to 1)))
    (is (= [] (primes-up-to 0))))
  
  (testing "Negative numbers"
    (is (false? (is-prime? -5)))
    (is (false? (is-prime? -1)))))

(run-tests)
