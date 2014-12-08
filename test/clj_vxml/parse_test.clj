(ns clj-vxml.parse-test
  (:require [clj-vxml.parse :refer :all]
            [midje.sweet :refer :all]))

(unfinished)

(facts "about parsing strings"
       (let [tests [{:name "Empty vxml doc"
                     :input "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<vxml version=\"2.1\" xmlns=\"http://www.w3.org/2001/vxml\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.w3.org/2001/vxml http://www.w3.org/TR/voicexml21/vxml.xsd\">
</vxml>"
                     :expected [:vxml {:version "2.1"
                                       :xsi/schemaLocation "http://www.w3.org/2001/vxml http://www.w3.org/TR/voicexml21/vxml.xsd"}]}
                    {:name "Block on it's own"
                     :input "<block></block>"
                     :expected [:block {}]}
                    {:name "Blocks can have audio"
                     :input "<block name=\"block-name\"><audio src=\"/path/to/prompt.wav\" /></block>"
                     :expected [:block {:name "block-name"} [:audio {:src "/path/to/prompt.wav"}]]}
                    {:name "Blocks can have prompts with TTS"
                     :input "<block><prompt><audio src=\"/path/to/prompt.wav\">Hello World</audio></prompt></block>"
                     :expected [:block {} [:prompt {} [:audio {:src "/path/to/prompt.wav"} "Hello World"]]]}]]
         (doseq [{:keys [name input expected]} tests]
           (fact {:midje/description name}
                 (parse-string input) => expected))))

(facts "About making dbs"
       (let [tests [{:name "Elements get an entity id"
                     :input [:block {}]
                     :expected [[1 :tag :block]]}
                    {:name "Elements can have attributes"
                     :input [:block {:name :block-name}]
                     :expected [[1 :tag :block]
                                [1 :name :block-name]]}
                    {:name "Elements can have a parent"
                     :input [:block {:name :block-name} [:prompt {}]]
                     :expected [[1 :tag :block]
                                [1 :name :block-name]
                                [2 :#parent 1]
                                [2 :tag :prompt]]}
                    {:name "Elements can have more than one child"
                     :input [:block {:name :block-name} [:prompt] [:audio]]
                     :expected [[1 :tag :block]
                                [1 :name :block-name]
                                [2 :#parent 1]
                                [2 :tag :prompt]
                                [3 :#parent 1]
                                [3 :#after 2]
                                [3 :tag :audio]]}]]
         (doseq [{:keys [name input expected]} tests]
           (fact {:midje/description name}
                 (:datums (hiccup->db input)) => expected))))
