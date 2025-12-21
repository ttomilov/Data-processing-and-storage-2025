(ns task-c3.core-test
  (:require [clojure.test :refer :all]
            [task-c3.core :refer [parallel-filter]]))

(deftest finite-seq-test
  (testing "Filters finite sequence"
    (is (= [2 4 6 8]
           (parallel-filter even? 2 [1 2 3 4 5 6 7 8])))))

(deftest infinite-seq-test
  (testing "Filters infinite sequence lazily"
    (is (= [0 2 4 6 8]
           (take 5 (parallel-filter even? 3 (range)))))))

(deftest correctness-test
  (testing "Matches standard filter"
    (let [data (range 100)]
      (is (= (filter odd? data)
             (parallel-filter odd? 10 data))))))

(deftest invalid-block-size-test
  (testing "Throws on non-positive block size"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"block-size"
                          (doall (parallel-filter odd? 0 [1 2 3]))))))

(deftest performance-test
  (testing "Parallel filtering is faster with blocking predicate"
    (let [data (range 40)
          slow-pred (fn [x]
                      (Thread/sleep 10)
                      (even? x))
          ;; warm-up to reduce JIT effects
          _ (doall (parallel-filter slow-pred 5 (range 10)))
          seq-start (System/nanoTime)
          _ (doall (filter slow-pred data))
          seq-ms (/ (- (System/nanoTime) seq-start) 1e6)
          par-start (System/nanoTime)
          _ (doall (parallel-filter slow-pred 5 data))
          par-ms (/ (- (System/nanoTime) par-start) 1e6)]
      (is (< par-ms (* 0.7 seq-ms))
          (str "Expected parallel filter to be faster; sequential " seq-ms " ms vs parallel " par-ms " ms")))))
