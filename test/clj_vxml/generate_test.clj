(ns clj-vxml.generate-test
  (:require [clj-vxml.generate :refer :all]
            [midje.sweet :refer :all]))

(facts "about generating strings"
       (let [tests [{:name "Empty vxml doc"
                     :expected "<?xml version=\"1.0\" encoding=\"UTF-8\"?><vxml xmlns=\"http://www.w3.org/2001/vxml\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.w3.org/2001/vxml http://www.w3.org/TR/voicexml21/vxml.xsd\" version=\"2.1\"/>
"
                     :input [:vxml {:version "2.1"
                                    :xsi:schemaLocation "http://www.w3.org/2001/vxml http://www.w3.org/TR/voicexml21/vxml.xsd"}]}
                    {:name "Block on it's own"
                     :expected "<?xml version=\"1.0\" encoding=\"UTF-8\"?><block/>
"
                     :input [:block {}]}
                    {:name "Blocks can have audio"
                     :expected "<?xml version=\"1.0\" encoding=\"UTF-8\"?><block name=\"block-name\">
  <audio src=\"/path/to/prompt.wav\"/>
</block>
"
                     :input [:block {:name "block-name"} [:audio {:src "/path/to/prompt.wav"}]]}
                    {:name "Blocks can have prompts with TTS"
                     :expected "<?xml version=\"1.0\" encoding=\"UTF-8\"?><block>
  <prompt>
    <audio src=\"/path/to/prompt.wav\">Hello World</audio>
  </prompt>
</block>
"
                     :input [:block {} [:prompt {} [:audio {:src "/path/to/prompt.wav"} "Hello World"]]]}]]
         (doseq [{:keys [name input expected]} tests]
           (fact {:midje/description name}
                 (generate-string input) => expected))))

;; Move this somewhere else
(facts "About generating hiccup"
       (let [tests [{:name "Elements get an entity id"
                     :expected [:block {}]
                     :input [[1 :tag :block]]}
                    {:name "Elements can have attributes"
                     :expected [:block {:name "block-name"}]
                     :input [[1 :tag :block]
                             [1 :name "block-name"]]}
                    {:name "Some elements are text"
                     :expected [:block {} "Hello World"]
                     :input [[1 :tag :block]
                             [2 :#parent 1]
                             [2 :content "Hello World"]]}
                    {:name "Elements can have a parent"
                     :expected [:block {:name "block-name"} [:prompt {}]]
                     :input [[1 :tag :block]
                             [1 :name "block-name"]
                             [2 :#parent 1]
                             [2 :tag :prompt]]}
                    {:name "Elements can have more than one child"
                     :expected [:block {:name "block-name"} [:prompt {}] [:audio {}]]
                     :input [[1 :tag :block]
                             [1 :name "block-name"]
                             [2 :#parent 1]
                             [2 :tag :prompt]
                             [3 :#parent 1]
                             [3 :#after 2]
                             [3 :tag :audio]]}]]
         (doseq [{:keys [name input expected]} tests]
           (fact {:midje/description name}
                 (db->hiccup input) => expected))))
