(ns auto-repair.store
  "SSoT for the ISCO-08 7231 independent auto-repair sole-proprietor actor,
  behind a `Store` protocol so the backend is a swap (MemStore default ‖ a
  real Datomic/kotoba-server backend, per the itonami actor pattern).

  Domain = independent auto repair practice:

    vehicle       — a serviced vehicle (vehicleId, vin)
    repair-order  — a service order scoped to a vehicle (orderId,
                    vehicleId, scope)
    repair-action — a repair action under an order (actionId, orderId,
                    kind #{:standard :engine-running})
    invoice       — a billed amount for an order (invoiceId, orderId,
                    amountCents)

  The append-only records are the operating ledger: a repair-action or
  invoice must reference a registered order on a registered vehicle, and
  repair-actions/invoices are never mutated in place, only appended.")

(defprotocol Store
  (vehicle [st vehicle-id])
  (repair-order [st order-id])
  (repair-orders-of [st vehicle-id])
  (repair-actions-of [st order-id])
  (invoices-of [st order-id])
  (register-vehicle! [st vehicle])
  (register-repair-order! [st repair-order])
  (record-repair-action! [st repair-action])
  (record-invoice! [st invoice]))

(defrecord MemStore [state]
  Store
  (vehicle [_ vehicle-id]
    (get-in @state [:vehicles vehicle-id]))
  (repair-order [_ order-id]
    (get-in @state [:repair-orders order-id]))
  (repair-orders-of [_ vehicle-id]
    (filter #(= vehicle-id (:vehicle-id %)) (vals (:repair-orders @state))))
  (repair-actions-of [_ order-id]
    (filter #(= order-id (:order-id %)) (:repair-actions @state)))
  (invoices-of [_ order-id]
    (filter #(= order-id (:order-id %)) (:invoices @state)))
  (register-vehicle! [_ vehicle]
    (swap! state assoc-in [:vehicles (:vehicle-id vehicle)] vehicle))
  (register-repair-order! [_ repair-order]
    (swap! state assoc-in [:repair-orders (:order-id repair-order)] repair-order))
  (record-repair-action! [_ repair-action]
    (swap! state update :repair-actions (fnil conj []) repair-action))
  (record-invoice! [_ invoice]
    (swap! state update :invoices (fnil conj []) invoice)))

(defn mem-store
  ([] (mem-store {}))
  ([seed]
   (->MemStore (atom (merge {:vehicles {} :repair-orders {} :repair-actions [] :invoices []} seed)))))
