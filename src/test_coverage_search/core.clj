(ns test-coverage-search.core
  (:require [hickory.core :as h]
            [hickory.select :as s]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.string :as str]))

(defn parse-file
  [path]
  (-> path
      slurp
      h/parse
      h/as-hickory))

(defn uncovered-elems
  [doc]
  (s/select (s/child (s/class "not-covered")) doc))

(defn is-event-producing?
  [codestr]
  (re-find #".*\(.*\/log-event.*|.*\(log-event.*|.*\(.*\/execute-db.*|.*\(execute-db.*" codestr))

(defn list-files
  [path]
  (map str
       (filter #(.isFile %) (file-seq (io/file path)))))

(defn pretty-spit
  [file-name data]
  (spit (java.io.File. file-name)
        (with-out-str (pp/write data :dispatch pp/code-dispatch))))

(defn uncovered-event-lines
  [filename]
  (->> filename
       parse-file
       uncovered-elems
       (map :content)
       (map first)
       (filter is-event-producing?)
       ; TODO include line numbers with results, need to split and read first number (leading zeros are ok)
       ; return as map with line number ?
       (map str/trim)))

(defn uncovered-event-code-by-filename
  [path]
  (into {} (keep (fn [f] (let [lines (uncovered-event-lines f)]
                           (if (seq lines)
                             {f (uncovered-event-lines f)}
                             nil)))
                 (list-files path))))

(comment
  (def coverage-dir "/Users/jamesvickers/Downloads/target/coverage/")
  (pretty-spit
   "/Users/jamesvickers/Downloads/missing_event_coverage.txt"
   (update-keys
    (uncovered-event-code-by-filename coverage-dir)
    #(.replace % coverage-dir ""))))
