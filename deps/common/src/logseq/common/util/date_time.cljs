(ns logseq.common.util.date-time
  "cljs-time util fns for deps"
  (:require [cljs-time.format :as tf]
            [clojure.string :as string]
            [logseq.common.util :as common-util]
            [cljs-time.core :as t]))

;; (tf/parse (tf/formatter "dd.MM.yyyy") "2021Q4") => 20040120T000000
(defn safe-journal-title-formatters
  [date-formatter]
  (->> [date-formatter "MMM do, yyyy" "yyyy-MM-dd" "yyyy_MM_dd"]
       (remove string/blank?)
       distinct))

(defn journal-title->
  [journal-title then-fn formatters]
  (when-not (string/blank? journal-title)
    (when-let [time' (->> (map
                           (fn [formatter]
                             (try
                               (tf/parse (tf/formatter formatter) (common-util/capitalize-all journal-title))
                               (catch :default _e
                                 nil)))
                           formatters)
                          (filter some?)
                          first)]
      (then-fn time'))))

(defn journal-title->int
  [journal-title formatters]
  (when journal-title
    (let [journal-title (common-util/capitalize-all journal-title)]
      (journal-title-> journal-title
                       #(parse-long (tf/unparse (tf/formatter "yyyyMMdd") %))
                       formatters))))

(defn format
  [date date-formatter]
  (when date-formatter
    (tf/unparse (tf/formatter date-formatter) date)))

(defn int->local-date
  [day]
  (let [s (str day)
        year (js/parseInt (subs s 0 4))
        month (js/parseInt (subs s 4 6))
        day (js/parseInt (subs s 6))]
    (t/local-date year month day)))

(defn int->journal-title
  [day date-formatter]
  (when day
    (format (int->local-date day) date-formatter)))

(defn- get-weekday
  [date]
  (.toLocaleString date "en-us" (clj->js {:weekday "long"})))

(defn- get-date
  ([]
   (get-date (js/Date.)))
  ([date]
   {:year (.getFullYear date)
    :month (inc (.getMonth date))
    :day (.getDate date)
    :weekday (get-weekday date)}))

(defn year-month-day-padded
  ([]
   (year-month-day-padded (get-date)))
  ([date]
   (let [{:keys [year month day]} date]
     {:year year
      :month (common-util/zero-pad month)
      :day (common-util/zero-pad day)})))

(defn ymd
  ([]
   (ymd (js/Date.)))
  ([date]
   (ymd date "/"))
  ([date sep]
   (let [{:keys [year month day]} (year-month-day-padded (get-date date))]
     (str year sep month sep day))))

(defn date->int
  "Given a date object, returns its journal page integer"
  [date]
  (parse-long
   (string/replace (ymd date) "/" "")))

(defn journal-day->ms
  "Convert :block/journal-day int to ms timestamp in current timezone"
  [journal-day]
  (let [journal-day' (str journal-day)
        year (js/parseInt (subs journal-day' 0 4))
        month (dec (js/parseInt (subs journal-day' 4 6)))
        day (js/parseInt (subs journal-day' 6 8))]
    (.getTime (new js/Date year month day))))
