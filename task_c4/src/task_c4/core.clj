(ns task-c4.core
  (:gen-class))

(declare supply-msg)
(declare notify-msg)

(defn storage
  "Creates a new storage
   ware - a name of ware to store (string)
   notify-step - amount of stored items required for logger to react. 0 means no logging
   consumers - factories to notify when the storage is updated
   returns a map that contains:
     :storage - an atom to store items that can be used by factories directly
     :ware - a stored ware name
     :worker - an agent to send supply-msg"
  [ware notify-step & consumers]
  (let [counter (atom 0 :validator #(>= % 0))
        worker-state {:storage counter
                      :ware ware
                      :notify-step notify-step
                      :consumers consumers}]
    {:storage counter
     :ware ware
     :worker (agent worker-state)}))

(defn factory
  "Creates a new factory
   amount - number of items produced per cycle
   duration - cycle duration in milliseconds
   target-storage - a storage to put products with supply-msg
   ware-amounts - a list of ware names and their amounts required for a single cycle
   returns a map that contains:
     :worker - an agent to send notify-msg"
  [amount duration target-storage & ware-amounts]
  (let [bill (apply hash-map ware-amounts)
        buffer (reduce-kv (fn [acc k _] (assoc acc k 0))
                          {} bill)
        ;; a state of factory agent:
        ;;  :produce-amount - a number of items to produce per cycle
        ;;  :duration - a duration of cycle
        ;;  :target-storage - a storage to place products (via supply-msg to its worker)
        ;;  :bill - a map with ware names as keys and their amounts of values
        ;;     shows how many wares must be consumed to perform one production cycle
        ;;  :buffer - a map with similar structure as for :bill that shows how many wares are already collected;
        ;;     it is the only mutable part.
        worker-state {:produce-amount amount
                      :duration duration
                      :target-storage target-storage
                      :bill bill
                      :buffer buffer}]
    {:worker (agent worker-state)}))

(defn source
  "Creates a source that is a thread that produces 'amount' of wares per cycle to store in 'target-storage'
   and with given cycle 'duration' in milliseconds
   returns Thread that must be run explicitly"
  [amount duration target-storage]
   (doto
    (Thread.
     (fn loop-src []
       (Thread/sleep duration)
       (send (target-storage :worker) supply-msg amount)
       (recur)))
     (.setDaemon true)))

(defn supply-msg
  "A message that can be sent to a storage worker to notify that the given 'amount' of wares should be added.
   Adds the given 'amount' of ware to the storage and notifies all the registered factories about it
   state - see code of 'storage' for structure"
  [state amount]
  (swap! (state :storage) #(+ % amount))      ; update counter, could not fail
  (let [ware (state :ware)
        cnt @(state :storage)
        notify-step (state :notify-step)
        consumers (state :consumers)]
    ;; logging part, notify-step == 0 means no logging
    (when (and (> notify-step 0)
               (> (int (/ cnt notify-step))
                  (int (/ (- cnt amount) notify-step))))
      (println (.format (java.text.SimpleDateFormat. "hh.mm.ss.SSS") (java.util.Date.))
               "|" ware "amount:" cnt))
    ;; factories notification part
    (when consumers
      (doseq [consumer (shuffle consumers)]
        (send (consumer :worker) notify-msg ware (state :storage) amount))))
  state)                 ; worker itself is immutable, keeping configuration only

(defn- try-take!
  "Atomically takes up to `n` items from `counter-atom`, never below zero. Returns actually taken count."
  [counter-atom n]
  (if (pos? n)
    (loop []
      (let [cur @counter-atom
            take (long (min cur n))
            new (- cur take)]
        (if (compare-and-set! counter-atom cur new)
          take
          (recur))))
    0))

(defn- enough-resources?
  "Checks if buffer has all required resources per bill."
  [buffer bill]
  (every? (fn [[ware need]]
            (>= (get buffer ware 0) need))
          bill))

(defn notify-msg
  "A message that can be sent to a factory worker to notify that the provided 'amount' of 'ware's are
   just put to the 'storage-atom'.
   Implements retrieval, buffering, production cycle, and notification to target storage.

   - tries to retrieve items from the storage atom (without going negative)
   - updates internal buffer
   - while buffer satisfies bill, performs production cycle (sleep duration), consumes bill, and supplies target
   - always returns updated state"
  [state ware storage-atom amount]
  (let [{:keys [bill buffer produce-amount duration target-storage]} state]
    (if-not (contains? bill ware)
      state                                     ; ignore wares this factory doesn't need
      (let [need (max 0 (- (bill ware) (get buffer ware 0)))
            initial-pull (try
                           (try-take! storage-atom (min need amount))
                           (catch Exception _ 0))
            buffer' (update buffer ware + initial-pull)
            ;; run as many production cycles as possible, topping up current ware each time from storage
            final-buffer (loop [buf buffer']
                           (if (enough-resources? buf bill)
                             (let [buf-after (reduce-kv (fn [b k need]
                                                          (update b k - need))
                                                        buf bill)
                                   missing (max 0 (- (bill ware) (get buf-after ware 0)))
                                   pulled (try
                                            (try-take! storage-atom missing)
                                            (catch Exception _ 0))
                                   buf-next (update buf-after ware + pulled)]
                               (Thread/sleep duration)
                               (send (target-storage :worker) supply-msg produce-amount)
                               (recur buf-next))
                             buf))]
        (assoc state :buffer final-buffer)))))

;; storages and factories wiring
(def safe-storage (storage "Safe" 1))
(def safe-factory (factory 1 3000 safe-storage "Metal" 3))
(def cuckoo-clock-storage (storage "Cuckoo-clock" 1))
(def cuckoo-clock-factory (factory 1 2000 cuckoo-clock-storage "Lumber" 5 "Gears" 10))
(def gears-storage (storage "Gears" 20 cuckoo-clock-factory))
(def gears-factory (factory 4 1000 gears-storage "Ore" 4))
(def metal-storage (storage "Metal" 5 safe-factory))
(def metal-factory (factory 1 1000 metal-storage "Ore" 10))
(def lumber-storage (storage "Lumber" 20 cuckoo-clock-factory))
(def lumber-mill (source 5 4000 lumber-storage))
(def ore-storage (storage "Ore" 10 metal-factory gears-factory))
(def ore-mine (source 2 1000 ore-storage))

;; runs sources and the whole process as the result
(defn start []
  (.start ore-mine)
  (.start lumber-mill))

;; stops running process
;; recompile the code after it to reset all the process
(defn stop []
  (.stop ore-mine)
  (.stop lumber-mill))

;; This could be used to acquire errors from workers
;; (agent-error (gears-factory :worker))
;; (agent-error (metal-storage :worker))

(defn -main
  [& _]
  (println "=== Запуск производственной линии ===")
  (println "Производятся часы и сейфы")
  (start)
  (Thread/sleep 30000)
  (println "=== Остановка производственной линии ===")
  (shutdown-agents)
  (println "=== Производство завершено ==="))