(ns clj-vxml.generate
  (:require [clojure.data.xml :as xml]
            [datomic.api :as d]))

(declare fix-vxml)

(defn trace
  [x]
  (prn x)
  x)

(defn generate-string
  [h]
  (-> h
      fix-vxml
      xml/sexp-as-element
      xml/indent-str))

(defn fix-vxml
  [[k a & c :as h]]
  (if (= :vxml k)
    (let [a (assoc a
              :xmlns:xsi "http://www.w3.org/2001/XMLSchema-instance"
              :xmlns "http://www.w3.org/2001/vxml")]
      (into [] (remove nil? [k a c])))
    h))

(defn missing?
  [db eid a]
  (let [e (ffirst (d/q '[:find ?e
                         :in $ ?e ?a
                         :where
                         [?e ?a]]
                       db eid a))]
    (nil? e)))

(defn get-root
  [db]
  (ffirst (d/q '[:find ?e
                 :where
                 [?e :tag]
                 [(clj-vxml.generate/missing? $ ?e :#parent)]]
               db)))

(defn element?
  [a]
  (not (#{:tag :#parent :#after :#content} a)))

(defn sibling
  [db eid]
  (ffirst (d/q '[:find ?s
                 :in $ ?e
                 :where
                 [?s :#after ?e]]
               db eid)))

(defn entity->hiccup
  [db eid]
  (if-let [tag (ffirst (d/q '[:find ?t
                              :in $ ?e
                              :where
                              [?e :tag ?t]]
                            db eid))]
    (let [attrs (into {} (d/q '[:find ?a ?v
                                :in $ ?e
                                :where
                                [?e ?a ?v]
                                [(clj-vxml.generate/element? ?a)]]
                              db eid))
          first-child (ffirst (d/q '[:find ?e
                                     :in $ ?p
                                     :where
                                     [?e :#parent ?p]
                                     [(clj-vxml.generate/missing? $ ?e :#after)]]
                                   db eid))
          children (take-while (complement nil?)
                               (iterate (partial sibling db)
                                        first-child))]
      (apply vector tag attrs (map (partial entity->hiccup db) children)))
    (let [content (ffirst (d/q '[:find ?c
                                 :in $ ?e
                                 :where
                                 [?e :content ?c]]
                               db eid))]
      content)))

(defn db->hiccup
  [db]
  (let [root (get-root db)]
    (entity->hiccup db root)))
