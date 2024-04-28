(ns frontend.worker.rtc.ws2
  "Websocket wrapped by missionary.
  based on
  https://github.com/ReilySiegel/missionary-websocket/blob/master/src/com/reilysiegel/missionary/websocket.cljs"
  {:clj-kondo/ignore true}
  (:require [frontend.worker.rtc.const :as rtc-const]
            [logseq.common.missionary-util :as c.m]
            [missionary.core :as m]))

(defn- open-ws-task
  [url]
  (fn [s! f!]
    (try
      (let [ws (js/WebSocket. url)]
        (set! (.-onopen ws)
              (fn [_]
                (let [mbx (m/mbx)
                      close-dfv (m/dfv)]
                  (set! (.-onopen ws) nil)
                  (set! (.-onmessage ws) (fn [e] (mbx (.-data e))))
                  (set! (.-onclose ws) (fn [e]
                                         (set! (.-onclose ws) nil)
                                         (close-dfv e)))
                  (s! [mbx ws close-dfv]))))
        (set! (.-onclose ws)
              (fn [e]
                (set! (.-onopen ws) nil)
                (set! (.-onclose ws) nil)
                (f! e)))
        (fn cancel-clean []
          (.close ws)))
      (catch :default e
        (f! e) #(do)))))

(defn- handle-close
  [x]
  (if (instance? js/CloseEvent x)
    (throw x)
    x))

(defn- create-ws-task
  [url]
  (m/sp
    (let [[mbx ws close-dfv] (m/? (open-ws-task url))]
      {:raw-ws ws
       :send (fn [data]
               (m/sp
                 (handle-close
                  (m/?
                   (m/race close-dfv
                           (m/sp (while (< 4096 (.-bufferedAmount ws))
                                   (m/? (m/sleep 50)))
                                 (.send ws data)))))))
       :recv-flow
       (m/stream
        (m/ap
          (loop []
            (m/amb
             (handle-close
              (m/? (m/race close-dfv mbx)))
             (recur)))))})))

(defn- get-state
  [ws]
  (case (.-readyState ws)
    0 :connecting
    1 :open
    2 :closing
    3 :closed))

(defn- closed?
  [m-ws]
  (contains? #{:closing :closed} (get-state (:raw-ws m-ws))))

(defn create-ws-flow
  "Return a missionary-webocket flow.
  Always produce NOT-closed websockets if possible.
  If open&connect websocket failed, will retry with backoff(`c.m/delays`)
  TODO: retry ASAP once network condition changed"
  [url]
  (let [*last-m-ws (atom nil)
        new-m-ws-task
        (c.m/backoff
         (take 10 c.m/delays)
         (m/sp (let [r (try
                         (m/? (create-ws-task url))
                         (catch js/CloseEvent e
                           (prn :throw-retry)
                           (throw (ex-info "retry" {:missionary/retry true} e))))]
                 (reset! *last-m-ws r)
                 r)))]
    (m/stream
     (m/ap
       (loop []
         (m/amb
          (if-let [m-ws @*last-m-ws]
            (if (closed? m-ws)
              (m/? new-m-ws-task)
              m-ws)
            (m/? new-m-ws-task))
          (recur)))))))

(defn close
  [m-ws]
  (.close (:raw-ws m-ws)))

(defn send-task
  "return m-ws"
  [ws-flow message]
  (m/sp
    (let [m-ws (m/? (m/reduce (fn [_ m-ws] (when m-ws (reduced m-ws))) ws-flow))
          decoded-message (rtc-const/data-to-ws-coercer message)
          message-str (js/JSON.stringify (clj->js (rtc-const/data-to-ws-encoder decoded-message)))]
      (m/? ((:send m-ws) message-str))
      m-ws)))

(defn recv-flow
  [m-ws]
  (m/eduction
   (map #(js->clj (js/JSON.parse %) :keywordize-keys true))
   (:recv-flow m-ws)))

(defn send&recv-task
  [ws-flow message & {:keys [timeout-ms] :or {timeout-ms 10000}}]
  (assert (pos-int? timeout-ms))
  (let [req-id (str (random-uuid))
        message (assoc message :req-id req-id)]
    (m/sp
      (let [m-ws (m/? (send-task ws-flow message))]
        (m/? (m/timeout
              (m/reduce
               (fn [_ v]
                 (when (= req-id (:req-id v))
                   (reduced v)))
               (recv-flow m-ws))
              timeout-ms))))))

(comment
  (do
    (def url "wss://ws-dev.logseq.com/rtc-sync?token=????")
    (def ws-flow (create-ws-flow url)))
  (def cancel1
    ((m/reduce (fn [_ v] (prn :v v)
                 (when (some? v)
                   (def m-ws v)
                   (reduced v))) (m/eduction (take 3) ws-flow)) #(prn :s %) #(prn :f %)))
  (cancel1)

  (do
    (def cancel ((m/join vector
                         (send&recv-task ws-flow {:action "list-graphs"} :timeout-ms 100)
                         (send&recv-task ws-flow {:action "list-graphs"}))
                 #(prn :s %) #(js/console.log :f %)))
    (cancel)))
