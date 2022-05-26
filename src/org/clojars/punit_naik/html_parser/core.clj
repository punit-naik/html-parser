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
      (some #(not (nil? (re-matches % warning)))
            allowed-warning-regexes)))

(defn parse-log-messages
  [error-output & allowed-warning-regexes]
  (keep
   (fn [error-text]
     (when-let [[line-column-type warning-message]
                (and (seq (re-matches #"line\s[0-9]+\scolumn\s[0-9]+\s\-\s[Warning|Error].*"
                                      error-text))
                     (str/split error-text #"\:\s"))]
       (let [[line-column type] (str/split line-column-type #"\s\-\s")
             type (str/lower-case type)
             [_ line _ column] (str/split line-column #"\s")
             allowed? (when (= type "warning")
                        (apply warning-allowed? warning-message allowed-warning-regexes))
             type (if (and (= type "warning")
                           (not allowed?))
                    "error" type)]
         {:message warning-message
          :type type
          :line (Integer/parseInt line)
          :column (Integer/parseInt column)})))
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
    (let [logs (apply parse-log-messages (str err-stream) allowed-warning-regexes)
          warnings (keep #(when (= (:type %) "warning")
                            (dissoc % :type)) logs)
          errors (keep #(when (= (:type %) "error")
                          (dissoc % :type)) logs)]
      (cond-> {:parsed-output (-> (str out-stream)
                                  str/trim-newline
                                  str/trim)}
        (seq errors) (assoc :errors errors)
        (seq warnings) (assoc :warnings warnings)))))
