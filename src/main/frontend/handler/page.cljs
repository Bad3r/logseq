(ns frontend.handler.page
  (:require [cljs.reader :as reader]
            [clojure.string :as string]
            [clojure.walk :as walk]
            [datascript.core :as d]
            [frontend.commands :as commands]
            [frontend.config :as config]
            [frontend.date :as date]
            [frontend.db :as db]
            [logseq.db.schema :as db-schema]
            [frontend.db.model :as model]
            [frontend.db.utils :as db-utils]
            [frontend.db.conn :as conn]
            [frontend.fs :as fs]
            [frontend.handler.common :as common-handler]
            [frontend.handler.editor :as editor-handler]
            [frontend.handler.notification :as notification]
            [frontend.handler.route :as route-handler]
            [frontend.handler.ui :as ui-handler]
            [frontend.handler.web.nfs :as web-nfs]
            [frontend.handler.config :as config-handler]
            [frontend.handler.recent :as recent-handler]
            [frontend.modules.outliner.core :as outliner-core]
            [frontend.modules.outliner.file :as outliner-file]
            [frontend.modules.outliner.tree :as outliner-tree]
            [frontend.state :as state]
            [frontend.util :as util]
            [frontend.util.cursor :as cursor]
            [frontend.util.property :as property]
            [frontend.util.page-property :as page-property]
            [goog.object :as gobj]
            [lambdaisland.glogi :as log]
            [promesa.core :as p]
            [frontend.mobile.util :as mobile-util]
            [logseq.graph-parser.util :as gp-util]
            [logseq.graph-parser.config :as gp-config]
            [logseq.graph-parser.block :as gp-block]
            [frontend.format.block :as block]
            [goog.functions :refer [debounce]]))

(defn- get-directory
  [journal?]
  (if journal?
    (config/get-journals-directory)
    (config/get-pages-directory)))

(defn- get-file-name
  [journal? title]
  (when-let [s (if journal?
                 (date/journal-title->default title)
                 (gp-util/page-name-sanity (string/lower-case title)))]
    ;; Win10 file path has a length limit of 260 chars
    (gp-util/safe-subs s 0 200)))

(defn get-page-file-path
  ([] (get-page-file-path (state/get-current-page)))
  ([page-name]
   (when page-name
     (let [page-name (util/page-name-sanity-lc page-name)]
       (when-let [page (db/entity [:block/name page-name])]
         (:file/path (:block/file page)))))))

(defn- build-title [page]
  (let [original-name (:block/original-name page)]
    (if (string/includes? original-name ",")
      (util/format "\"%s\"" original-name)
      original-name)))

(defn default-properties-block
  ([title format page]
   (default-properties-block title format page {}))
  ([title format page properties]
   (let [p (common-handler/get-page-default-properties title)
         ps (merge p properties)
         content (page-property/insert-properties format "" ps)
         refs (gp-block/get-page-refs-from-properties properties
                                                      (db/get-db (state/get-current-repo))
                                                      (state/get-date-formatter))]
     {:block/uuid (db/new-block-id)
      :block/properties ps
      :block/properties-order (keys ps)
      :block/refs refs
      :block/left page
      :block/format format
      :block/content content
      :block/parent page
      :block/page page})))

(defn- create-title-property?
  [journal? page-name]
  (and (not journal?)
       (util/create-title-property? page-name)))

(defn- build-page-tx [format properties page journal?]
  (when (:block/uuid page)
    (let [page-entity [:block/uuid (:block/uuid page)]
          create-title? (create-title-property? journal?
                                                (or
                                                 (:block/original-name page)
                                                 (:block/name page)))
          page (if (seq properties) (assoc page :block/properties properties) page)
          page-empty? (db/page-empty? (state/get-current-repo) (:block/name page))]
      (cond
        (not page-empty?)
        [page]

        create-title?
        (let [properties-block (default-properties-block (build-title page) format page-entity properties)]
          [page
           properties-block])

        (seq properties)
        [page (editor-handler/properties-block properties format page-entity)]

        :else
        [page]))))

(defn create!
  "Create page.
   :redirect?           - when true, redirect to the created page, otherwise return sanitized page name.
   :split-namespace?    - when true, split hierarchical namespace into levels.
   :create-first-block? - when true, create an empty block if the page is empty.
   :uuid                - when set, use this uuid instead of generating a new one."
  ([title]
   (create! title {}))
  ([title {:keys [redirect? create-first-block? format properties split-namespace? journal? uuid]
           :or   {redirect?           true
                  create-first-block? true
                  format              nil
                  properties          nil
                  split-namespace?    true
                  uuid                nil}}]
   (let [title      (string/trim title)
         title      (gp-util/remove-boundary-slashes title)
         page-name  (util/page-name-sanity-lc title)
         repo       (state/get-current-repo)
         with-uuid? (if (uuid? uuid) uuid true)] ;; FIXME: prettier validation
     (when (db/page-empty? repo page-name)
       (let [pages    (if split-namespace?
                        (gp-util/split-namespace-pages title)
                        [title])
             format   (or format (state/get-preferred-format))
             pages    (map (fn [page]
                             ;; only apply uuid to the deepest hierarchy of page to create if provided.
                             (-> (block/page-name->map page (if (= page title) with-uuid? true))
                                 (assoc :block/format format)))
                           pages)
             txs      (->> pages
                           ;; for namespace pages, only last page need properties
                           drop-last
                           (mapcat #(build-page-tx format nil % journal?))
                           (remove nil?)
                           (remove (fn [m]
                                     (some? (db/entity [:block/name (:block/name m)])))))
             last-txs (build-page-tx format properties (last pages) journal?)
             txs      (concat txs last-txs)]
         (when (seq txs)
           (db/transact! txs)))

       (when create-first-block?
         (when (or
                (db/page-empty? repo (:db/id (db/entity [:block/name page-name])))
                (create-title-property? journal? page-name))
           (editor-handler/api-insert-new-block! "" {:page page-name}))))

     (when redirect?
       (route-handler/redirect-to-page! page-name))
     page-name)))

(defn delete-file!
  [repo page-name unlink-file?]
  (let [file (db/get-page-file page-name)
        file-path (:file/path file)]
    ;; delete file
    (when-not (string/blank? file-path)
      (db/transact! [[:db.fn/retractEntity [:file/path file-path]]])
      (when unlink-file?
        (-> (fs/unlink! repo (config/get-repo-path repo file-path) nil)
            (p/catch (fn [error] (js/console.error error))))))))

(defn- compute-new-file-path
  [old-path new-name]
  (let [result (string/split old-path "/")
        file-name (gp-util/page-name-sanity new-name true)
        ext (last (string/split (last result) "."))
        new-file (str file-name "." ext)
        parts (concat (butlast result) [new-file])]
    (string/join "/" parts)))

(defn rename-file!
  [file new-name ok-handler]
  (let [repo (state/get-current-repo)
        file (db/pull (:db/id file))
        old-path (:file/path file)
        new-path (compute-new-file-path old-path new-name)]
    ;; update db
    (db/transact! repo [{:db/id (:db/id file)
                         :file/path new-path}])
    (->
     (p/let [_ (fs/rename! repo old-path new-path)]
       (ok-handler))
     (p/catch (fn [error]
                (println "file rename failed: " error))))))

(defn- replace-page-ref!
  "Unsanitized names"
  [content old-name new-name]
  (let [[original-old-name original-new-name] (map string/trim [old-name new-name])
        [old-ref new-ref] (map #(util/format "[[%s]]" %) [old-name new-name])
        [old-name new-name] (map #(if (string/includes? % "/")
                                    (string/replace % "/" ".")
                                    %)
                                 [original-old-name original-new-name])
        old-org-ref (and (= :org (state/get-preferred-format))
                         (:org-mode/insert-file-link? (state/get-config))
                         (re-find
                          (re-pattern
                           (util/format
                            "\\[\\[file:\\.*/.*%s\\.org\\]\\[(.*?)\\]\\]" old-name))
                          content))]
    (-> (if old-org-ref
          (let [[old-full-ref old-label] old-org-ref
                new-label (if (= old-label original-old-name)
                            original-new-name
                            old-label)
                new-full-ref (-> (string/replace old-full-ref old-name new-name)
                                 (string/replace (str "[" old-label "]")
                                                 (str "[" new-label "]")))]
            (string/replace content old-full-ref new-full-ref))
          content)
        (string/replace old-ref new-ref))))

(defn- replace-tag-ref!
  [content old-name new-name]
  (let [old-tag (util/format "#%s" old-name)
        new-tag (if (re-find #"[\s\t]+" new-name)
                  (util/format "#[[%s]]" new-name)
                  (str "#" new-name))]
    (-> (util/replace-ignore-case content (str "^" old-tag "\\b") new-tag)
        (util/replace-ignore-case (str " " old-tag " ") (str " " new-tag " "))
        (util/replace-ignore-case (str " " old-tag "$") (str " " new-tag)))))

(defn- replace-old-page!
  "Unsanitized names"
  [content old-name new-name]
  (when (and (string? content) (string? old-name) (string? new-name))
    (-> content
        (replace-page-ref! old-name new-name)
        (replace-tag-ref! old-name new-name))))

(defn- walk-replace-old-page!
  "Unsanitized names"
  [form old-name new-name]
  (walk/postwalk (fn [f]
                   (cond
                     (and (vector? f)
                          (contains? #{"Search" "Label"} (first f))
                          (string/starts-with? (second f) (str old-name "/")))
                     [(first f) (string/replace-first (second f)
                                                      (str old-name "/")
                                                      (str new-name "/"))]

                     (string? f)
                     (if (= f old-name)
                       new-name
                       (replace-old-page! f old-name new-name))

                     :else
                     f))
                 form))

(defn favorited?
  [page-name]
  (let [favorites (->> (:favorites (state/get-config))
                       (filter string?)
                       (map string/lower-case)
                       (set))]
    (contains? favorites page-name)))

(defn favorite-page!
  [page-name]
  (when-not (string/blank? page-name)
    (let [favorites (->
                     (cons
                      page-name
                      (or (:favorites (state/get-config)) []))
                     (distinct)
                     (vec))]
      (config-handler/set-config! :favorites favorites))))

(defn unfavorite-page!
  [page-name]
  (when-not (string/blank? page-name)
    (let [favorites (->> (:favorites (state/get-config))
                         (remove #(= (string/lower-case %) (string/lower-case page-name)))
                         (vec))]
      (config-handler/set-config! :favorites favorites))))

(defn toggle-favorite! []
  ;; NOTE: in journals or settings, current-page is nil
  (when-let [page-name (state/get-current-page)]
   (let [favorites  (:favorites (state/sub-graph-config))
         favorited? (contains? (set (map string/lower-case favorites))
                               (string/lower-case page-name))]
    (if favorited?
      (unfavorite-page! page-name)
      (favorite-page! page-name)))))

(defn delete!
  [page-name ok-handler & {:keys [delete-file?]
                           :or {delete-file? true}}]
  (when page-name
    (when-let [repo (state/get-current-repo)]
      (let [page-name (util/page-name-sanity-lc page-name)
            blocks (db/get-page-blocks-no-cache page-name)
            tx-data (mapv
                     (fn [block]
                       [:db.fn/retractEntity [:block/uuid (:block/uuid block)]])
                     blocks)
            page (db/entity [:block/name page-name])]
        (db/transact! tx-data)

        (delete-file! repo page-name delete-file?)

        ;; if other page alias this pagename,
        ;; then just remove some attrs of this entity instead of retractEntity
        (when-not (:block/_namespace page)
          (if (model/get-alias-source-page (state/get-current-repo) page-name)
            (when-let [id (:db/id (db/entity [:block/name page-name]))]
              (let [txs (mapv (fn [attribute]
                                [:db/retract id attribute])
                              db-schema/retract-page-attributes)]
                (db/transact! txs)))
            (db/transact! [[:db.fn/retractEntity [:block/name page-name]]])))

        (unfavorite-page! page-name)

        (when (fn? ok-handler) (ok-handler))
        (ui-handler/re-render-root!)))))

(defn- rename-update-block-refs!
  [refs from-id to-id]
  (->> refs
       (remove #{{:db/id from-id}})
       (cons {:db/id to-id})
       (distinct)
       (vec)))

(defn- rename-update-refs!
  "Unsanitized only"
  [page old-original-name new-name]
  ;; update all pages which have references to this page
  (let [repo (state/get-current-repo)
        to-page (db/entity [:block/name (util/page-name-sanity-lc new-name)])
        blocks   (db/get-page-referenced-blocks-no-cache (:db/id page))
        page-ids (->> (map :block/page blocks)
                      (remove nil?)
                      (set))
        tx       (->> (map (fn [{:block/keys [uuid content properties] :as block}]
                             (let [content    (let [content' (replace-old-page! content old-original-name new-name)]
                                                (when-not (= content' content)
                                                  content'))
                                   properties (let [properties' (walk-replace-old-page! properties old-original-name new-name)]
                                                (when-not (= properties' properties)
                                                  properties'))]
                               (when (or content properties)
                                 (util/remove-nils-non-nested
                                  {:block/uuid       uuid
                                   :block/content    content
                                   :block/properties properties
                                   :block/refs (rename-update-block-refs! (:block/refs block) (:db/id page) (:db/id to-page))
                                   :block/path-refs (rename-update-block-refs! (:block/path-refs block) (:db/id page) (:db/id to-page))})))) blocks)
                      (remove nil?))]
    (db/transact! repo tx)
    (doseq [page-id page-ids]
      (outliner-file/sync-to-file page-id))))

(defn- rename-page-aux
  "Only accepts unsanitized page names"
  [old-name new-name redirect?]
  (let [old-page-name       (util/page-name-sanity-lc old-name)
        new-file-name       (util/file-name-sanity new-name)
        new-page-name       (util/page-name-sanity-lc new-name)
        repo                (state/get-current-repo)
        page                (db/pull [:block/name old-page-name])]
    (when (and repo page)
      (let [old-original-name   (:block/original-name page)
            file                (:block/file page)
            journal?            (:block/journal? page)
            properties-block    (:data (outliner-tree/-get-down (outliner-core/block page)))
            properties-block-tx (when (and properties-block
                                           (string/includes? (util/page-name-sanity-lc (:block/content properties-block))
                                                             old-page-name))
                                  (let [front-matter? (and (property/front-matter? (:block/content properties-block))
                                                           (= :markdown (:block/format properties-block)))]
                                    {:db/id         (:db/id properties-block)
                                     :block/content (property/insert-property (:block/format properties-block)
                                                                              (:block/content properties-block)
                                                                              :title
                                                                              new-name
                                                                              front-matter?)}))
            page-txs            [{:db/id               (:db/id page)
                                  :block/uuid          (:block/uuid page)
                                  :block/name          new-page-name
                                  :block/original-name new-name}]
            page-txs            (if properties-block-tx (conj page-txs properties-block-tx) page-txs)]

        (d/transact! (db/get-db repo false) page-txs)

        ;; If page name changed after sanitization
        (when (or (util/create-title-property? new-page-name)
                  (not= (gp-util/page-name-sanity new-name false) new-name))
          (page-property/add-property! new-page-name :title new-name))

        (when (and file (not journal?))
          (rename-file! file new-file-name (fn [] nil)))

        (rename-update-refs! page old-original-name new-name)

        (outliner-file/sync-to-file page))

      ;; Redirect to the newly renamed page
      (when redirect?
        (route-handler/redirect! {:to          :page
                                  :push        false
                                  :path-params {:name new-page-name}}))

      (when (favorited? old-page-name)
        (p/do!
         (unfavorite-page! old-page-name)
         (favorite-page! new-page-name)))

      (recent-handler/update-or-add-renamed-page repo old-page-name new-page-name)

      (ui-handler/re-render-root!))))

(defn- rename-nested-pages
  "Unsanitized names only"
  [old-ns-name new-ns-name]
  (let [repo            (state/get-current-repo)
        nested-page-str (util/format "[[%s]]" (util/page-name-sanity-lc old-ns-name))
        ns-prefix       (util/format "[[%s/" (util/page-name-sanity-lc old-ns-name))
        nested-pages    (db/get-pages-by-name-partition repo nested-page-str)
        nested-pages-ns (db/get-pages-by-name-partition repo ns-prefix)]
    (when nested-pages
      ;; rename page "[[obsidian]] is a tool" to "[[logseq]] is a tool"
      (doseq [{:block/keys [name original-name]} nested-pages]
        (let [old-page-title (or original-name name)
              new-page-title (string/replace
                              old-page-title
                              (util/format "[[%s]]" old-ns-name)
                              (util/format "[[%s]]" new-ns-name))]
          (when (and old-page-title new-page-title)
            (p/do!
             (rename-page-aux old-page-title new-page-title false)
             (println "Renamed " old-page-title " to " new-page-title))))))
    (when nested-pages-ns
      ;; rename page "[[obsidian/page1]] is a tool" to "[[logseq/page1]] is a tool"
      (doseq [{:block/keys [name original-name]} nested-pages-ns]
        (let [old-page-title (or original-name name)
              new-page-title (string/replace
                              old-page-title
                              (util/format "[[%s/" old-ns-name)
                              (util/format "[[%s/" new-ns-name))]
          (when (and old-page-title new-page-title)
            (p/do!
             (rename-page-aux old-page-title new-page-title false)
             (println "Renamed " old-page-title " to " new-page-title))))))))

(defn- rename-namespace-pages!
  "Original names (unsanitized only)"
  [repo old-name new-name]
  (let [pages (db/get-namespace-pages repo old-name)
        page (db/pull [:block/name (util/page-name-sanity-lc old-name)])
        pages (cons page pages)]
    (doseq [{:block/keys [name original-name]} pages]
      (let [old-page-title (or original-name name)
            new-page-title (string/replace old-page-title old-name new-name)
            redirect? (= name (:block/name page))]
        (when (and old-page-title new-page-title)
          (p/let [_ (rename-page-aux old-page-title new-page-title redirect?)]
            (println "Renamed " old-page-title " to " new-page-title)))))))

(defn merge-pages!
  "Only accepts sanitized page names"
  [from-page-name to-page-name]
  (when (and (db/page-exists? from-page-name)
             (db/page-exists? to-page-name)
             (not= from-page-name to-page-name))
    (let [to-page (db/entity [:block/name to-page-name])
          to-id (:db/id to-page)
          from-page (db/entity [:block/name from-page-name])
          from-id (:db/id from-page)
          from-first-child (some->> (db/pull from-id)
                                    (outliner-core/block)
                                    (outliner-tree/-get-down)
                                    (outliner-core/get-data))
          to-last-direct-child-id (model/get-block-last-direct-child (db/get-db) to-id false)
          repo (state/get-current-repo)
          conn (conn/get-db repo false)
          datoms (d/datoms @conn :avet :block/page from-id)
          block-eids (mapv :e datoms)
          blocks (db-utils/pull-many repo '[:db/id :block/page :block/refs :block/path-refs :block/left :block/parent] block-eids)
          tx-data (map (fn [block]
                         (let [id (:db/id block)]
                           (cond->
                            {:db/id id
                             :block/page {:db/id to-id}
                             :block/path-refs (rename-update-block-refs! (:block/path-refs block) from-id to-id)
                             :block/refs (rename-update-block-refs! (:block/refs block) from-id to-id)}

                             (and from-first-child (= id (:db/id from-first-child)))
                             (assoc :block/left {:db/id (or to-last-direct-child-id to-id)})

                             (= (:block/parent block) {:db/id from-id})
                             (assoc :block/parent {:db/id to-id})))) blocks)]
      (d/transact! conn tx-data)
      (outliner-file/sync-to-file {:db/id to-id})

      (rename-update-refs! from-page
                           (util/get-page-original-name from-page)
                           (util/get-page-original-name to-page)))

    (delete! from-page-name nil)

    (route-handler/redirect! {:to          :page
                              :push        false
                              :path-params {:name to-page-name}})))

(defn rename!
  "Accepts unsanitized page names"
  [old-name new-name]
  (let [repo          (state/get-current-repo)
        old-name      (string/trim old-name)
        new-name      (string/trim new-name)
        old-page-name (util/page-name-sanity-lc old-name)
        new-page-name (util/page-name-sanity-lc new-name)
        name-changed? (not= old-name new-name)]
    (if (and old-name
             new-name
             (not (string/blank? new-name))
             name-changed?)
      (do
        (cond
          (= old-page-name new-page-name)
          (rename-page-aux old-name new-name true)

          (db/pull [:block/name new-page-name])
          (merge-pages! old-page-name new-page-name)

          :else
          (rename-namespace-pages! repo old-name new-name))
        (rename-nested-pages old-name new-name))
      (when (string/blank? new-name)
        (notification/show! "Please use a valid name, empty name is not allowed!" :error)))))

(defn- split-col-by-element
  [col element]
  (let [col (vec col)
        idx (.indexOf col element)]
    [(subvec col 0 (inc idx))
     (subvec col (inc idx))]))

(defn reorder-favorites!
  [{:keys [to up?]}]
  (let [favorites (:favorites (state/get-config))
        from (get @state/state :favorites/dragging)]
    (when (and from to (not= from to))
      (let [[prev next] (split-col-by-element favorites to)
            [prev next] (mapv #(remove (fn [e] (= from e)) %) [prev next])
            favorites (->>
                       (if up?
                         (concat (drop-last prev) [from (last prev)] next)
                         (concat prev [from] next))
                       (remove nil?)
                       (distinct))]
        (config-handler/set-config! :favorites favorites)))))

(defn has-more-journals?
  []
  (let [current-length (:journals-length @state/state)]
    (< current-length (db/get-journals-length))))

(defn load-more-journals!
  []
  (when (has-more-journals?)
    (state/set-journals-length! (+ (:journals-length @state/state) 7))))

(defn update-public-attribute!
  [page-name value]
  (page-property/add-property! page-name :public value))

(defn get-page-ref-text
  [page]
  (let [edit-block-file-path (model/get-block-file-path (state/get-edit-block))
        page-name (string/lower-case page)]
    (if (and edit-block-file-path
             (state/org-mode-file-link? (state/get-current-repo)))
      (if-let [ref-file-path (:file/path (db/get-page-file page-name))]
        (util/format "[[file:%s][%s]]"
                     (util/get-relative-path edit-block-file-path ref-file-path)
                     page)
        (let [journal? (date/valid-journal-title? page)
              ref-file-path (str
                             (if (or (util/electron?) (mobile-util/native-platform?))
                               (-> (config/get-repo-dir (state/get-current-repo))
                                   js/decodeURI
                                   (string/replace #"/+$" "")
                                   (str "/"))
                               "")
                             (get-directory journal?)
                             "/"
                             (get-file-name journal? page)
                             ".org")]
          (create! page {:redirect? false})
          (util/format "[[file:%s][%s]]"
                       (util/get-relative-path edit-block-file-path ref-file-path)
                       page)))
      (util/format "[[%s]]" page))))

(defn init-commands!
  []
  (commands/init-commands! get-page-ref-text))

(def rebuild-slash-commands-list!
  (debounce init-commands! 1500))

(defn template-exists?
  [title]
  (when title
    (let [templates (keys (db/get-all-templates))]
      (when (seq templates)
        (let [templates (map string/lower-case templates)]
          (contains? (set templates) (string/lower-case title)))))))

(defn ls-dir-files!
  [ok-handler]
  (web-nfs/ls-dir-files-with-handler!
   (fn []
     (init-commands!)
     (when ok-handler (ok-handler)))))

(defn get-all-pages
  [repo]
  (->> (db/get-all-pages repo)
       (remove (fn [p]
                 (let [name (:block/name p)]
                   (or (util/uuid-string? name)
                       (gp-config/draw? name)
                       (db/built-in-pages-names (string/upper-case name))))))
       (common-handler/fix-pages-timestamps)))

(defn get-filters
  [page-name]
  (let [properties (db/get-page-properties page-name)
        properties-str (get properties :filters "{}")]
    (try (reader/read-string properties-str)
         (catch js/Error e
           (log/error :syntax/filters e)))))

(defn save-filter!
  [page-name filter-state]
  (page-property/add-property! page-name :filters filter-state))

;; Editor
(defn page-not-exists-handler
  [input id q current-pos]
  (state/set-editor-show-page-search! false)
  (if (state/org-mode-file-link? (state/get-current-repo))
    (let [page-ref-text (get-page-ref-text q)
          value (gobj/get input "value")
          old-page-ref (util/format "[[%s]]" q)
          new-value (string/replace value
                                    old-page-ref
                                    page-ref-text)]
      (state/set-edit-content! id new-value)
      (let [new-pos (+ current-pos
                       (- (count page-ref-text)
                          (count old-page-ref))
                       2)]
        (cursor/move-cursor-to input new-pos)))
    (let [current-selected (util/get-selected-text)]
      (cursor/move-cursor-forward input (+ 2 (count current-selected))))))

(defn on-chosen-handler
  [input id _q pos format]
  (let [current-pos (cursor/pos input)
        edit-content (state/sub [:editor/content id])
        q (or
           @editor-handler/*selected-text
           (when (state/sub :editor/show-page-search-hashtag?)
             (gp-util/safe-subs edit-content pos current-pos))
           (when (> (count edit-content) current-pos)
             (gp-util/safe-subs edit-content pos current-pos)))]
    (if (state/sub :editor/show-page-search-hashtag?)
      (fn [chosen _click?]
        (state/set-editor-show-page-search! false)
        (let [wrapped? (= "[[" (gp-util/safe-subs edit-content (- pos 2) pos))
              chosen (if (string/starts-with? chosen "New page: ") ;; FIXME: What if a page named "New page: XXX"?
                       (subs chosen 10)
                       chosen)
              chosen (if (and (util/safe-re-find #"\s+" chosen) (not wrapped?))
                       (util/format "[[%s]]" chosen)
                       chosen)
              q (if @editor-handler/*selected-text "" q)
              [last-pattern forward-pos] (if wrapped?
                                           [q 3]
                                           (if (= \# (first q))
                                             [(subs q 1) 1]
                                             [q 2]))
              last-pattern (str "#" (when wrapped? "[[") last-pattern)]
          (editor-handler/insert-command! id
                                          (str "#" (when wrapped? "[[") chosen)
                                          format
                                          {:last-pattern last-pattern
                                           :end-pattern (when wrapped? "]]")
                                           :forward-pos forward-pos})))
      (fn [chosen _click?]
        (state/set-editor-show-page-search! false)
        (let [chosen (if (string/starts-with? chosen "New page: ")
                       (subs chosen 10)
                       chosen)
              page-ref-text (get-page-ref-text chosen)]
          (editor-handler/insert-command! id
                                          page-ref-text
                                          format
                                          {:last-pattern (str "[[" (if @editor-handler/*selected-text "" q))
                                           :end-pattern "]]"
                                           :postfix-fn   (fn [s] (util/replace-first "]]" s ""))
                                           :forward-pos 3}))))))

(defn create-today-journal!
  []
  (when-let [repo (state/get-current-repo)]
    (when (and (state/enable-journals? repo)
               (not (state/loading-files? repo)))
      (state/set-today! (date/today))
      (when (or (config/local-db? repo)
                (and (= "local" repo) (not (mobile-util/native-platform?))))
        (let [title (date/today)
              today-page (util/page-name-sanity-lc title)
              format (state/get-preferred-format repo)
              file-name (date/journal-title->default title)
              path (str (config/get-journals-directory) "/" file-name "."
                        (config/get-file-extension format))
              file-path (str "/" path)
              repo-dir (config/get-repo-dir repo)
              template (state/get-default-journal-template)]
          (p/let [file-exists? (fs/file-exists? repo-dir file-path)
                  file-content (when file-exists?
                                 (fs/read-file repo-dir file-path))]
            (when (and (db/page-empty? repo today-page)
                       (or (not file-exists?)
                           (and file-exists? (string/blank? file-content))))
              (create! title {:redirect? false
                              :split-namespace? false
                              :create-first-block? (not template)
                              :journal? true})
              (state/pub-event! [:journal/insert-template today-page])
              (ui-handler/re-render-root!))))))))

(defn open-today-in-sidebar
  []
  (when-let [page (db/entity [:block/name (util/page-name-sanity-lc (date/today))])]
    (state/sidebar-add-block!
     (state/get-current-repo)
     (:db/id page)
     :page)))

(defn open-file-in-default-app []
  (when-let [file-path (and (util/electron?) (get-page-file-path))]
    (js/window.apis.openPath file-path)))

(defn copy-current-file []
  (when-let [file-path (and (util/electron?) (get-page-file-path))]
    (util/copy-to-clipboard! file-path)))

(defn open-file-in-directory []
  (when-let [file-path (and (util/electron?) (get-page-file-path))]
    (js/window.apis.showItemInFolder file-path)))
