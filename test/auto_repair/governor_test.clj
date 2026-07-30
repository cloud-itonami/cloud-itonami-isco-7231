(ns auto-repair.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [auto-repair.store :as store]
            [auto-repair.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-vehicle! st {:vehicle-id "veh-1" :vin "1HGCM82633A004352"})
    (store/register-repair-order! st {:order-id "order-1" :vehicle-id "veh-1" :scope "brake inspection"})
    st))

(deftest proceeds-on-clean-standard-repair
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:kind :standard :order-id "order-1" :safety-class :low
                   :effect :propose :confidence 0.9}]
    (is (= :proceed (:decision (governor/assess env proposal))))))

(deftest holds-on-unregistered-order
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:kind :standard :order-id "no-such-order" :safety-class :low
                   :effect :propose :confidence 0.9}
        result (governor/assess env proposal)]
    (is (= :hold (:decision result)))
    (is (some #(= :no-order (:rule %)) (:violations result)))))

(deftest holds-on-no-actuation-violation
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:kind :standard :order-id "order-1" :safety-class :low
                   :effect :direct-write :confidence 0.9}
        result (governor/assess env proposal)]
    (is (= :hold (:decision result)))
    (is (some #(= :no-actuation (:rule %)) (:violations result)))))

(deftest holds-on-engine-running-repair-without-high-safety-class
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:kind :engine-running :order-id "order-1" :safety-class :medium
                   :effect :propose :confidence 0.9}
        result (governor/assess env proposal)]
    (is (= :hold (:decision result)))
    (is (some #(= :engine-running-safety (:rule %)) (:violations result)))))

(deftest human-approval-on-engine-running-repair-with-high-safety-class
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:kind :engine-running :order-id "order-1" :safety-class :high
                   :effect :propose :confidence 0.9}]
    (is (= :human-approval (:decision (governor/assess env proposal))))))

(deftest human-approval-on-low-confidence
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:kind :standard :order-id "order-1" :safety-class :none
                   :effect :propose :confidence 0.2}
        result (governor/assess env proposal)]
    (is (= :human-approval (:decision result)))
    (is (= :low-confidence (:reason result)))))

(deftest store-records-append-only
  (let [st (fresh-store)]
    (store/record-repair-action! st {:action-id "a1" :order-id "order-1" :kind :standard})
    (store/record-invoice! st {:invoice-id "i1" :order-id "order-1" :amount-cents 8500})
    (is (= 1 (count (store/repair-actions-of st "order-1"))))
    (is (= 1 (count (store/invoices-of st "order-1"))))
    (is (= 1 (count (store/repair-orders-of st "veh-1"))))))

(deftest a-proposal-without-confidence-does-not-proceed
  (testing "確信度を言っていない提案は、確信していると言っていないので auto-proceed
            させない。この既定は 2026-07-30 まで 1.0 で、:confidence を持たない提案が
            :proceed していた（ADR-2607309100）。fleet の boolean 方言 346 件はすべて
            0.0 既定で、うち isco-5419 はそれを明示的にテストしている。"
    (let [st (fresh-store)
          env (governor/env-for-store st)
          proposal {:kind :standard :order-id "order-1" :safety-class :low :effect :propose}
          result (governor/assess env proposal)]
      (is (= 0.0 (:confidence result))
          "欠落した :confidence は 0.0 であって 1.0 ではない")
      (is (not= :proceed (:decision result))
          "確信度不明の提案が自動で通ってはならない"))))
