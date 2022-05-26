(ns org.clojars.punit-naik.html-parser.core
  (:require [clojure.string :as str])
  (:import [java.io
            ByteArrayInputStream
            ByteArrayOutputStream
            PrintWriter]
           [org.w3c.tidy Tidy]))

(defn warning-allowed?
  "Checks if the warning string as allowed warning which should not be deemed as an error"
  [warning & allowed-warning-regexes]
  (or (str/starts-with? warning "plain text isn't allowed in")
      (str/starts-with? warning "unknown attribute")
      (= warning "inserting missing 'title' element")
      (= warning "missing <!DOCTYPE> declaration")
      (some #(not (nil? (re-matches % warning)))
            allowed-warning-regexes)))

(defn parse-warning-messages
  [error-output & allowed-warning-regexes]
  (keep
   (fn [error-text]
     (when-let [warning-message (and (seq (re-matches #"line\s[0-9]+\scolumn\s[0-9]+.*"
                                                      error-text))
                                     (second (str/split error-text #"\:\s")))]
       {:allowed? (apply warning-allowed? warning-message allowed-warning-regexes)
        :message warning-message}))
   (str/split error-output #"\n")))

(defn create-error-writer
  [& [stream]]
  (PrintWriter.
   (or stream
       (ByteArrayOutputStream.))
   true))

(defn create-tidy-object
  []
  (let [^Tidy tidy (Tidy.)]
    (.setPrintBodyOnly tidy true)
    (.setXHTML tidy true)
    tidy))

(defn parse-html
  "Parses an html string and returns the parsed body
   Also returns all warning messages and whether they are allowed or not
   And also the error count which is parser errors + disallowed warnings count"
  [tidy-obj html-string & allowed-warning-regexes]
  (with-open [in-stream (ByteArrayInputStream. (.getBytes html-string))
              out-stream (ByteArrayOutputStream.)
              err-stream (ByteArrayOutputStream.)
              err-writer (create-error-writer err-stream)]
    (.setErrout tidy-obj err-writer)
    (.parse tidy-obj in-stream out-stream)
    (let [warnings (apply parse-warning-messages (str err-stream) allowed-warning-regexes)]
      {:parsed-output (-> (str out-stream)
                          str/trim-newline
                          str/trim)
       :error-count (+ (.getParseErrors tidy-obj)
                       (count (remove :allowed? warnings)))
       :warnings warnings})))
