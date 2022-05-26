(defproject org.clojars.punit-naik/html-parser "1.0.2"
  :description "A Clojure library designed to parse HTML string and return any errors and warnings while parsing"
  :url "https://github.com/punit-naik/html-parser"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [com.github.jtidy/jtidy "1.0.2"]]
  :repl-options {:init-ns org.clojars.punit-naik.html-parser.core})
