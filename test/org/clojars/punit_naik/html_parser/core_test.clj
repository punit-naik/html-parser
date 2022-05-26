(ns org.clojars.punit-naik.html-parser.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [org.clojars.punit-naik.html-parser.core :as hp]))

(deftest warning-allowed?-test
  (testing "Several warning messages and seeing if they are allowed"
    (is (hp/warning-allowed? "plain text isn't allowed in <head> elements"))
    (is (hp/warning-allowed? "unknown attribute \"test\""))
    (is (hp/warning-allowed? "inserting missing 'title' element"))
    (is (hp/warning-allowed? "missing <!DOCTYPE> declaration"))
    (is (not (hp/warning-allowed? "missing </div>")))
    (is (not (hp/warning-allowed? "missing </a>")))))

(deftest parse-warning-messages-test
  (testing "If the warning messages from parser error output are parsed properly"
    (is (= (hp/parse-warning-messages
            (str "line 1 column 1 - Warning: unknown attribute \"asdsdas\"\n"
                 "line 1 column 31 - Warning: missing </div>\n"
                 "line 1 column 32 - Warning: inserting missing 'title' element\n"
                 "InputStream: Document content looks like HTML 4.01\n"
                 "3 warnings, no errors were found!\n"))
           [{:message "unknown attribute \"asdsdas\"", :allowed? true}
            {:message "missing </div>", :allowed? false}
            {:message "inserting missing 'title' element", :allowed? true}]))))

(deftest parse-html-test
  (testing "If HTML is parsed correctly"
    (is (= (hp/parse-html (hp/create-tidy-object) "<div>punit naik</div>")
           {:parsed-output "<div>punit naik</div>"
            :error-count 0
            :warnings
            [{:message "inserting missing 'title' element", :allowed? true}]}))
    (is (= (hp/parse-html (hp/create-tidy-object) "<div>punit naik")
           {:parsed-output "<div>punit naik</div>",
            :error-count 1,
            :warnings
            [{:message "missing </div>", :allowed? false}
             {:message "inserting missing 'title' element", :allowed? true}]}))
    (is (= (hp/parse-html (hp/create-tidy-object) "<dev>punit naik</div>")
           {:parsed-output "",
            :error-count 4,
            :warnings
            [{:message "<dev> is not recognized!", :allowed? false}
             {:message "discarding unexpected <dev>", :allowed? false}
             {:message "plain text isn't allowed in <head> elements",
              :allowed? true}
             {:message "discarding unexpected </div>", :allowed? false}
             {:message "inserting missing 'title' element", :allowed? true}]}))))