(ns main
  (:require [babashka.process :as proc]
            [uix.core :as uix]
            [uix.dev :as udev]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [org.jsoup Jsoup]
           [org.jsoup.nodes Element]
           [org.jsoup.parser Tag Parser ParseSettings]))

(def fontawesome-url  "https://github.com/FortAwesome/Font-Awesome/archive/refs/heads/6.x.zip" )
(def output-dir "./src/uix_fontawesome")
(def tmp-dir "./out")

(defn set-dimension [data]
  (apply list (update (into [] data) 2
                      (fn [params]
                        (merge params
                               {:width 40
                                :height 32})))))

(defn html->uix [str]
  (->> (-> (Parser/htmlParser)
           (.settings ParseSettings/preserveCase)
           (.parseFragmentInput str (Element. (Tag/valueOf "body") "") ""))
       (map hickory.core/as-hiccup)
       (map udev/from-hiccup)))

(defn from-html [name str]
  `(~'defui ~(symbol name) []
    ~(set-dimension (first (html->uix str)))))

(defn prelude [dir-name]
  `((~'ns ~(symbol (str "uix-fontawesome." dir-name))
     (:require [~'uix.core :refer [~'defui ~'$]]))))

(defn remove-suffix [name]
  (->> (str/split name #"\.")
       drop-last
       (str/join ".")))

(defn sanitize-name [name]
  (str "_" name))

(defn init []
  (proc/shell "mkdir -p" output-dir)
  (proc/shell "sh -c" (str "rm -rf " output-dir "/*"))
  (proc/shell "mkdir -p" tmp-dir)
  (proc/shell "sh -c" (str "rm -rf " tmp-dir "/*"))
  (proc/shell "curl" "-L" fontawesome-url "-o" (str tmp-dir "/fontawesome.zip"))
  (proc/shell "unzip" (str tmp-dir "/fontawesome.zip") "-d" (str tmp-dir "/")))

(defn list-files-in-directory [dir-path]
  (let [dir (io/file dir-path)]
    (when (.isDirectory dir)
      (into [] (.listFiles dir)))))

(defn generate [& _]
  (init)
  (doseq [dir (.listFiles (io/file (str tmp-dir "/Font-Awesome-6.x/svgs/")))]
    (let [dir-name (.getName dir)]
      (->> (for [icon (.listFiles dir)]
             (from-html (sanitize-name (remove-suffix (.getName icon))) (slurp icon)))
           (concat (prelude dir-name))
           (map pr-str)
           (str/join "\n")
           (spit (str output-dir "/" dir-name ".cljc"))))))

