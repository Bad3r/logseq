(ns logseq.db
  "Main namespace for public db fns. For DB and file graphs.
   For shared file graph only fns, use logseq.graph-parser.db"
  (:require [datascript.core :as d]
            [clojure.string :as string]
            [logseq.common.util :as common-util]
            [logseq.common.config :as common-config]
            [logseq.db.frontend.rules :as rules]
            [logseq.db.frontend.entity-plus :as entity-plus]
            [logseq.db.sqlite.util :as sqlite-util]
            [logseq.db.sqlite.common-db :as sqlite-common-db]
            [logseq.db.frontend.delete-blocks :as delete-blocks]
            [datascript.impl.entity :as de]))

;; Use it as an input argument for datalog queries
(def block-attrs
  '[:db/id
    :block/uuid
    :block/parent
    :block/order
    :block/collapsed?
    :block/collapsed-properties
    :block/format
    :block/refs
    :block/_refs
    :block/path-refs
    :block/tags
    :block/link
    :block/content
    :block/marker
    :block/priority
    :block/properties
    :block/properties-order
    :block/properties-text-values
    :block/pre-block?
    :block/scheduled
    :block/deadline
    :block/repeated?
    :block/created-at
    :block/updated-at
    ;; TODO: remove this in later releases
    :block/heading-level
    :block/file
    :class/parent
    {:block/page [:db/id :block/name :block/original-name :block/journal-day]}
    {:block/_parent ...}])

(defonce *transact-fn (atom nil))
(defn register-transact-fn!
  [f]
  (when f (reset! *transact-fn f)))

(defn transact!
  "`repo-or-conn`: repo for UI thread and conn for worker/node"
  ([repo-or-conn tx-data]
   (transact! repo-or-conn tx-data nil))
  ([repo-or-conn tx-data tx-meta]
   (let [tx-data (map (fn [m]
                        (if (map? m)
                          (dissoc m :block/children :block/meta :block/top? :block/bottom? :block/anchor
                                  :block/title :block/body :block/level :block/container :db/other-tx
                                  :block/unordered)
                          m)) tx-data)
         tx-data (->> (common-util/fast-remove-nils tx-data)
                      (remove empty?))
         delete-blocks-tx (when-not (string? repo-or-conn)
                            (delete-blocks/update-refs-and-macros @repo-or-conn tx-data tx-meta))
         tx-data (concat tx-data delete-blocks-tx)]

     ;; Ensure worker can handle the request sequentially (one by one)
     ;; Because UI assumes that the in-memory db has all the data except the last one transaction
     (when (seq tx-data)

       ;; (prn :debug :transact :sync? (= d/transact! (or @*transact-fn d/transact!)) :tx-meta tx-meta)
       ;; (cljs.pprint/pprint tx-data)

       (let [f (or @*transact-fn d/transact!)]
         (try
           (f repo-or-conn tx-data tx-meta)
           (catch :default e
             (js/console.trace)
             (prn :debug-tx-data tx-data)
             (throw e))))))))

(defn sort-by-order
  [blocks]
  (sort-by :block/order blocks))

(defn- get-block-and-children-aux
  [entity]
  (if-let [children (:block/_parent entity)]
    (cons (dissoc entity :block/_parent) (mapcat get-block-and-children-aux children))
    [entity]))

(defn get-block-and-children
  [db block-uuid]
  (when-let [e (d/entity db [:block/uuid block-uuid])]
    (get-block-and-children-aux e)))

(defn whiteboard-page?
  "Given a page entity or map, check if it is a whiteboard page"
  [page]
  (contains? (set (:block/type page)) "whiteboard"))

(defn journal-page?
  "Given a page entity or map, check if it is a journal page"
  [page]
  (contains? (set (:block/type page)) "journal"))

(defn get-page-blocks
  "Return blocks of the designated page, without using cache.
   page-id - eid"
  [db page-id {:keys [pull-keys]
               :or {pull-keys '[*]}}]
  (when page-id
    (let [datoms (d/datoms db :avet :block/page page-id)
          block-eids (mapv :e datoms)]
      (d/pull-many db pull-keys block-eids))))

(defn get-page-blocks-count
  [db page-id]
  (count (d/datoms db :avet :block/page page-id)))

(defn- get-block-children-or-property-children
  [block parent]
  (let [from-property (:logseq.property/created-from-property block)
        closed-property (:block/closed-value-property block)]
    (sort-by-order (cond
                     closed-property
                     (:property/closed-values closed-property)

                     from-property
                     (filter (fn [e]
                               (= (:db/id (:logseq.property/created-from-property e))
                                  (:db/id from-property)))
                             (:block/_raw-parent parent))

                     :else
                     (:block/_parent parent)))))

(defn get-right-sibling
  [block]
  (assert (or (de/entity? block) (nil? block)))
  (when-let [parent (:block/parent block)]
    (let [children (get-block-children-or-property-children block parent)
          right (some (fn [child] (when (> (compare (:block/order child) (:block/order block)) 0) child)) children)]
      (when (not= (:db/id right) (:db/id block))
        right))))

(defn get-left-sibling
  [block]
  (assert (or (de/entity? block) (nil? block)))
  (when-let [parent (:block/parent block)]
    (let [children (reverse (get-block-children-or-property-children block parent))
          left (some (fn [child] (when (< (compare (:block/order child) (:block/order block)) 0) child)) children)]
      (when (not= (:db/id left) (:db/id block))
        left))))

(defn get-down
  [block]
  (assert (or (de/entity? block) (nil? block)))
  (first (sort-by-order (:block/_parent block))))


(defn hidden-page?
  [page]
  (when page
    (if (string? page)
      (or (string/starts-with? page "$$$")
          (= common-config/favorites-page-name page))
      (contains? (set (:block/type page)) "hidden"))))

(defn get-pages
  [db]
  (->> (d/q
        '[:find ?page-original-name
          :where
          [?page :block/name ?page-name]
          [(get-else $ ?page :block/original-name ?page-name) ?page-original-name]]
        db)
       (map first)
       (remove hidden-page?)))

(def get-first-page-by-name sqlite-common-db/get-first-page-by-name)

(defn page-exists?
  "Whether a page exists."
  [db page-name]
  (when page-name
    (some? (get-first-page-by-name db page-name))))

(defn get-page
  "Get a page given its unsanitized name"
  [db page-name-or-uuid]
  (when db
    (if-let [id (if (uuid? page-name-or-uuid) page-name-or-uuid
                    (parse-uuid page-name-or-uuid))]
      (d/entity db [:block/uuid id])
      (d/entity db (get-first-page-by-name db (name page-name-or-uuid))))))

(defn get-case-page
  "Case sensitive version of get-page. For use with DB graphs"
  [db page-name-or-uuid]
  (when db
    (if-let [id (if (uuid? page-name-or-uuid) page-name-or-uuid
                    (parse-uuid page-name-or-uuid))]
      (d/entity db [:block/uuid id])
      (d/entity db (sqlite-common-db/get-first-page-by-original-name db page-name-or-uuid)))))

(defn page-empty?
  "Whether a page is empty. Does it has a non-page block?
  `page-id` could be either a string or a db/id."
  [db page-id]
  (let [page-id (if (string? page-id)
                  (get-first-page-by-name db page-id)
                  page-id)
        page (d/entity db page-id)]
    (empty? (:block/_parent page))))

(defn get-first-child
  [db id]
  (first (sort-by-order (:block/_parent (d/entity db id)))))

(defn get-orphaned-pages
  [db {:keys [pages empty-ref-f built-in-pages-names]
       :or {empty-ref-f (fn [page] (zero? (count (:block/_refs page))))
            built-in-pages-names #{}}}]
  (let [pages (->> (or pages (get-pages db))
                   (remove nil?))
        built-in-pages (set (map string/lower-case built-in-pages-names))
        orphaned-pages (->>
                        (map
                         (fn [page]
                           (when-let [page (get-page db page)]
                             (let [name (:block/name page)]
                               (and
                                (empty-ref-f page)
                                (or
                                 (page-empty? db (:db/id page))
                                 (let [first-child (get-first-child db (:db/id page))
                                       children (:block/_page page)]
                                   (and
                                    first-child
                                    (= 1 (count children))
                                    (contains? #{"" "-" "*"} (string/trim (:block/content first-child))))))
                                (not (contains? built-in-pages name))
                                (not (whiteboard-page? page))
                                (not (:block/_namespace page))
                                (not (contains? (:block/type page) "property"))
                                 ;; a/b/c might be deleted but a/b/c/d still exists (for backward compatibility)
                                (not (and (string/includes? name "/")
                                          (not (journal-page? page))))
                                page))))
                         pages)
                        (remove false?)
                        (remove nil?)
                        (remove hidden-page?))]
    orphaned-pages))

(defn has-children?
  [db block-id]
  (some? (:block/_parent (d/entity db [:block/uuid block-id]))))

(defn- collapsed-and-has-children?
  [db block]
  (and (:block/collapsed? block) (has-children? db (:block/uuid block))))

(defn get-block-last-direct-child-id
  "Notice: if `not-collapsed?` is true, will skip searching for any collapsed block."
  ([db db-id]
   (get-block-last-direct-child-id db db-id false))
  ([db db-id not-collapsed?]
   (when-let [block (d/entity db db-id)]
     (when (if not-collapsed?
             (not (collapsed-and-has-children? db block))
             true)
       (let [children (sort-by :block/order (:block/_parent block))]
         (:db/id (last children)))))))

(defn get-children
  "Doesn't include nested children."
  ([block-entity]
   (get-children nil block-entity))
  ([db block-entity-or-eid]
   (when-let [parent (cond
                       (number? block-entity-or-eid)
                       (d/entity db block-entity-or-eid)
                       (uuid? block-entity-or-eid)
                       (d/entity db [:block/uuid block-entity-or-eid])
                       :else
                       block-entity-or-eid)]
     (sort-by-order (:block/_parent parent)))))

(defn get-block-parents
  [db block-id {:keys [depth] :or {depth 100}}]
  (loop [block-id block-id
         parents (list)
         d 1]
    (if (> d depth)
      parents
      (if-let [parent (:block/parent (d/entity db [:block/uuid block-id]))]
        (recur (:block/uuid parent) (conj parents parent) (inc d))
        parents))))

(def get-block-children-ids sqlite-common-db/get-block-children-ids)
(def get-block-children sqlite-common-db/get-block-children)

(defn- get-sorted-page-block-ids
  [db page-id]
  (let [root (d/entity db page-id)]
    (loop [result []
           children (sort-by-order (:block/_parent root))]
      (if (seq children)
        (let [child (first children)]
          (recur (conj result (:db/id child))
                 (concat
                  (sort-by-order (:block/_parent child))
                  (rest children))))
        result))))

(defn sort-page-random-blocks
  "Blocks could be non consecutive."
  [db blocks]
  (assert (every? #(= (:block/page %) (:block/page (first blocks))) blocks) "Blocks must to be in a same page.")
  (let [page-id (:db/id (:block/page (first blocks)))
        ;; TODO: there's no need to sort all the blocks
        sorted-ids (get-sorted-page-block-ids db page-id)
        blocks-map (zipmap (map :db/id blocks) blocks)]
    (keep blocks-map sorted-ids)))

(defn last-child-block?
  "The child block could be collapsed."
  [db parent-id child-id]
  (when-let [child (d/entity db child-id)]
    (cond
      (= parent-id child-id)
      true

      (get-right-sibling child)
      false

      :else
      (last-child-block? db parent-id (:db/id (:block/parent child))))))

(defn- consecutive-block?
  [db block-1 block-2]
  (let [aux-fn (fn [block-1 block-2]
                 (and (= (:block/page block-1) (:block/page block-2))
                      (or
                       ;; sibling or child
                       (= (:db/id (get-left-sibling block-2)) (:db/id block-1))
                       (when-let [prev-sibling (get-left-sibling (d/entity db (:db/id block-2)))]
                         (last-child-block? db (:db/id prev-sibling) (:db/id block-1))))))]
    (or (aux-fn block-1 block-2) (aux-fn block-2 block-1))))

(defn get-non-consecutive-blocks
  [db blocks]
  (vec
   (keep-indexed
    (fn [i _block]
      (when (< (inc i) (count blocks))
        (when-not (consecutive-block? db (nth blocks i) (nth blocks (inc i)))
          (nth blocks i))))
    blocks)))

(defn new-block-id
  []
  (d/squuid))

(defn get-tag-blocks
  [db tag-name]
  (d/q '[:find [?b ...]
         :in $ ?tag
         :where
         [?e :block/name ?tag]
         [?b :block/tags ?e]]
       db
       (common-util/page-name-sanity-lc tag-name)))

(defn get-classes-with-property
  "Get classes which have given property as a class property"
  [db property-id]
  (:class/_schema.properties (d/entity db property-id)))


(defn get-alias-source-page
  "return the source page (page-name) of an alias"
  [db alias-id]
  (when alias-id
      ;; may be a case that a user added same alias into multiple pages.
      ;; only return the first result for idiot-proof
    (first (:block/_alias (d/entity db alias-id)))))

(defn get-page-alias
  [db page-id]
  (->>
   (d/q
    '[:find [?e ...]
      :in $ ?page %
      :where
      (alias ?page ?e)]
    db
    page-id
    (:alias rules/rules))
   distinct))

(defn get-page-refs
  [db id]
  (let [alias (->> (get-page-alias db id)
                   (cons id)
                   distinct)
        refs (->> (mapcat (fn [id] (:block/_path-refs (d/entity db id))) alias)
                  distinct)]
    (when (seq refs)
      (d/pull-many db '[*] (map :db/id refs)))))

(defn get-block-refs
  [db id]
  (let [block (d/entity db id)]
    (if (:block/name block)
      (get-page-refs db id)
      (let [refs (:block/_refs (d/entity db id))]
        (when (seq refs)
          (d/pull-many db '[*] (map :db/id refs)))))))

(defn get-block-refs-count
  [db id]
  (some-> (d/entity db id)
          :block/_refs
          count))

(defn get-page-unlinked-refs
  "Get unlinked refs from search result"
  [db page-id search-result-eids]
  (let [alias (->> (get-page-alias db page-id)
                   (cons page-id)
                   set)
        eids (remove
              (fn [eid]
                (when-let [e (d/entity db eid)]
                  (or (some alias (map :db/id (:block/refs e)))
                      (:block/link e)
                      (nil? (:block/content e)))))
              search-result-eids)]
    (when (seq eids)
      (d/pull-many db '[*] eids))))

(defn built-in?
  "Built-in page or block"
  [entity]
  (:logseq.property/built-in? entity))

(defn built-in-class-property?
  "Whether property a built-in property for the specific class"
  [class-entity property-entity]
  (and (built-in? class-entity)
       (contains? (:block/type class-entity) "class")
       (built-in? property-entity)
       (contains? (set (map :db/ident (:class/schema.properties class-entity)))
                  (:db/ident property-entity))))

(def write-transit-str sqlite-util/write-transit-str)
(def read-transit-str sqlite-util/read-transit-str)

(defn create-favorites-page
  "Creates hidden favorites page for storing favorites"
  [repo]
  (transact!
   repo
   [(sqlite-util/block-with-timestamps
     {:block/uuid (d/squuid)
      :block/name common-config/favorites-page-name
      :block/original-name common-config/favorites-page-name
      :block/type #{"hidden"}
      :block/format :markdown})]))

(defn build-favorite-tx
  "Builds tx for a favorite block in favorite page"
  [favorite-uuid]
  {:block/link [:block/uuid favorite-uuid]
   :block/content ""
   :block/format :markdown})

;; TODO: why not generate a UUID for all local graphs?
;; And prefer this local graph UUID when picking an ID for new rtc graph?
(defn get-graph-rtc-uuid
  [db]
  (when db (:kv/value (d/entity db :logseq.kv/graph-uuid))))

(def page? sqlite-util/page?)
(defn class?
  [entity]
  (contains? (:block/type entity) "class"))
(defn property?
  [entity]
  (contains? (:block/type entity) "property"))
(defn closed-value?
  [entity]
  (contains? (:block/type entity) "closed value"))

(def db-based-graph? entity-plus/db-based-graph?)

;; File based fns
(defn get-namespace-pages
  "Accepts both sanitized and unsanitized namespaces"
  [db namespace {:keys [db-graph?]}]
  (assert (string? namespace))
  (let [namespace (common-util/page-name-sanity-lc namespace)
        pull-attrs  (cond-> [:db/id :block/name :block/original-name :block/namespace]
                      (not db-graph?)
                      (conj {:block/file [:db/id :file/path]}))]
    (d/q
     [:find [(list 'pull '?c pull-attrs) '...]
      :in '$ '% '?namespace
      :where
      ['?p :block/name '?namespace]
      (list 'namespace '?p '?c)]
     db
     (:namespace rules/rules)
     namespace)))

(defn get-pages-by-name-partition
  [db partition]
  (when-not (string/blank? partition)
    (let [partition (common-util/page-name-sanity-lc (string/trim partition))
          ids (->> (d/datoms db :aevt :block/name)
                   (filter (fn [datom]
                             (let [page (:v datom)]
                               (string/includes? page partition))))
                   (map :e))]
      (when (seq ids)
        (d/pull-many db
                     '[:db/id :block/name :block/original-name]
                     ids)))))

(defn get-all-properties
  [db]
  (->> (d/datoms db :avet :block/type "property")
       (map (fn [d]
              (d/entity db (:e d))))))
