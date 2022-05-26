(ns org.clojars.punit-naik.html-parser.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [org.clojars.punit-naik.html-parser.core :as hp]))

(deftest warning-allowed?-test
  (testing "Several warning messages and seeing if they are allowed"
    (is (hp/warning-allowed? "plain text isn't allowed in <head> elements"))
    (is (hp/warning-allowed? "unknown attribute \"test\""))
    (is (hp/warning-allowed? "inserting missing 'title' element"))
    (is (hp/warning-allowed? "missing </a>" #"missing\s\<\/[a-z]+\>"))
    (is (not (hp/warning-allowed? "missing </div>")))
    (is (not (hp/warning-allowed? "missing </a>")))))

(deftest parse-log-messages-test
  (testing "If the warning messages from parser error output are parsed properly"
    (is (= (hp/parse-log-messages
            (str "line 1 column 1 - Warning: unknown attribute \"asdsdas\"\n"
                 "line 1 column 31 - Warning: missing </div>\n"
                 "line 1 column 32 - Warning: inserting missing 'title' element\n"
                 "InputStream: Document content looks like HTML 4.01\n"
                 "3 warnings, no errors were found!\n"))
           [{:message "unknown attribute \"asdsdas\""
             :type "warning"
             :line 1
             :column 1}
            {:message "missing </div>"
             :type "error"
             :line 1
             :column 31}
            {:message "inserting missing 'title' element"
             :type "warning"
             :line 1
             :column 32}]))
    (is (= (hp/parse-log-messages
            (str "line 1 column 1 - Warning: unknown attribute \"asdsdas\"\n"
                 "line 1 column 31 - Warning: missing </div>\n"
                 "line 1 column 32 - Warning: inserting missing 'title' element\n"
                 "InputStream: Document content looks like HTML 4.01\n"
                 "3 warnings, no errors were found!\n")
            #"missing\s\<\/div\>")
           [{:message "unknown attribute \"asdsdas\""
             :type "warning"
             :line 1
             :column 1}
            {:message "missing </div>"
             :type "warning"
             :line 1
             :column 31}
            {:message "inserting missing 'title' element"
             :type "warning"
             :line 1
             :column 32}]))))

(deftest parse-html-test
  (testing "If HTML is parsed correctly"
    (is (= (hp/parse-html (hp/create-tidy-object) "<div>punit naik</div>")
           {:parsed-output "<div>punit naik</div>"
            :warnings
            [{:message "inserting missing 'title' element"
              :line 1
              :column 22}]}))
    (is (= (hp/parse-html (hp/create-tidy-object) "<div>punit naik")
           {:parsed-output "<div>punit naik</div>"
            :errors
            [{:message "missing </div>"
              :line 1
              :column 16}]
            :warnings
            [{:message "inserting missing 'title' element"
              :line 1
              :column 17}]}))
    (is (= (hp/parse-html (hp/create-tidy-object) "<dev>punit naik</div>")
           {:parsed-output ""
            :errors
            [{:message "<dev> is not recognized!"
              :line 1
              :column 1}
             {:message "discarding unexpected <dev>"
              :line 1
              :column 1}
             {:message "discarding unexpected </div>"
              :line 1
              :column 16}]
            :warnings
            [{:message "plain text isn't allowed in <head> elements"
              :line 1
              :column 4}
             {:message "inserting missing 'title' element"
              :line 1
              :column 22}]}))
    (is (= (hp/parse-html (hp/create-tidy-object) "<div>punit naik" #"missing\s\<\/div\>")
           {:parsed-output "<div>punit naik</div>"
            :warnings
            [{:message "missing </div>"
              :line 1
              :column 16}
             {:message "inserting missing 'title' element"
              :line 1
              :column 17}]}))))