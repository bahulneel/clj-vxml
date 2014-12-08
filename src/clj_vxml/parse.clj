(ns clj-vxml.parse
  (:require [clojure.data.xml :as xml]))

(declare element->hiccup hiccup->vxml)

(defn parse-string
  [s]
  (let [xml (xml/parse-str s :namespace-aware true)]
    (element->hiccup xml)))

(defn element->hiccup
  [e]
  (if (string? e)
    e
    (let [{:keys [tag attrs content]} e
          content (map element->hiccup content)]
      (apply vector tag attrs content))))

(defn add-element
  [db k a parent-id]
  (let [{:keys [next-id last-id]} db
        datums (map (fn [[n v]]
                      [next-id n v]) a)
        datums (conj datums [next-id :tag k])
        datums (if (and parent-id (not= parent-id last-id))
                 (conj datums [next-id :#after last-id])
                 datums)
        datums (if parent-id
                 (conj datums [next-id :#parent parent-id])
                 datums)]
    (-> db
        (assoc :last-id next-id)
        (update-in [:next-id] inc)
        (update-in [:datums] (partial apply conj) datums))))

(defn hiccup->db
  ([e]
     (hiccup->db e {:next-id 1 :datums []}))
  ([e db]
     (hiccup->db e db nil))
  ([e db last-id]
     (let [[k a & c] e
           db (add-element db k a last-id)
           last-id (:last-id db)]
       (reduce (fn [db e] (hiccup->db e db last-id)) db c))))
