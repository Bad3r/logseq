(ns frontend.dicts.zh-hant
  "Provides translation to zh-Hant")

(def ^:large-vars/data-var dicts
  {:accessibility/skip-to-main-content "跳轉到主頁面"
   :on-boarding/demo-graph "目前在 demo 用圖表，您需要打開本機目錄以保存。"
   :on-boarding/add-graph "增加圖表"
   :on-boarding/open-local-dir "打開本機目錄"
   :on-boarding/new-graph-desc-1 "Logseq 支援 Markdown 和 Org-mode 兩種編輯模式。您可以打開本機目錄或創建新目錄。您的資料將僅儲存在目前設備。"
   :on-boarding/new-graph-desc-2 "在您打開目錄後，它會在該目錄中創建三個文件夾，分別為："
   :on-boarding/new-graph-desc-3 "/journals - 儲存日記專用頁面"
   :on-boarding/new-graph-desc-4 "/pages - 儲存其他頁面"
   :on-boarding/new-graph-desc-5 "/logseq - 儲存所有設定檔、CSS 和各種 metadata"
   :on-boarding/welcome-whiteboard-modal-title "屬於你的思考輔助畫布工具"
   :on-boarding/welcome-whiteboard-modal-description "白板是一個用將來發散的思緒整理起來的工具。您可以將任何想法放在白板上，再將彼此之間連接、理解和吸收。"
   :on-boarding/welcome-whiteboard-modal-skip "略過"
   :on-boarding/welcome-whiteboard-modal-start "開始"
   :on-boarding/tour-whiteboard-home "{1} 白板目錄"
   :on-boarding/tour-whiteboard-home-description "你可以利用此目錄瀏覽、新增、刪除、歸檔所有白板。"
   :on-boarding/tour-whiteboard-new "{1} 創建新白板"
   :on-boarding/tour-whiteboard-new-description "除了這裡還有很多方法能創建新白板，詳閱 LogSeq 文件。"
   :help/start "入門"
   :help/about "關於 Logseq"
   :help/roadmap "路線圖"
   :help/bug "問題回報"
   :help/feature "功能建議"
   :help/changelog "更新紀錄"
   :help/blog "Logseq 部落格"
   :help/docs "文件"
   :help/privacy "隱私聲明"
   :help/terms "服務條款"
   :help/forum-community "論壇社群"
   :help/awesome-logseq "很棒的 Logseq"
   :help/shortcuts "快捷鍵"
   :help/shortcuts-triggers "觸發"
   :help/shortcut "快捷鍵"
   :help/slash-autocomplete "Slash 自動"
   :help/block-content-autocomplete "區塊內容自動完成"
   :help/reference-autocomplete "頁面引用自動完成"
   :help/block-reference "區塊引用"
   :help/open-link-in-sidebar "在側邊欄開啟連結r"
   :more "更多"
   :search/result-for "正在搜尋 "
   :search/items "物件"
   :search/page-names "搜尋分頁名稱"
   :search-item/whiteboard "白板"
   :search-item/page "頁面"
   :search-item/file "文件"
   :search-item/block "區塊"
   :help/context-menu "區塊內容選單"
   :help/markdown-syntax "Markdown 格式"
   :help/org-mode-syntax "Org-mode 格式"
   :bold "粗體"
   :italics "斜體"
   :highlight "高亮"
   :strikethrough "刪除線"
   :code "程式"
   :untitled "無標題文件"
   :right-side-bar/help "幫助"
   :right-side-bar/switch-theme "主題模式"
   :right-side-bar/contents "內容"
   :right-side-bar/page-graph "頁面圖表"
   :right-side-bar/block-ref "區塊引用"
   :right-side-bar/graph-view "圖表顯示"
   :right-side-bar/all-pages "所有頁面"
   :right-side-bar/whiteboards "白板"
   :right-side-bar/flashcards "卡片"
   :right-side-bar/new-page "新頁面"
   :right-side-bar/show-journals "顯示日記頁面"
   :right-side-bar/separator "右側邊欄調整大小"
   :left-side-bar/journals "日記頁面"
   :left-side-bar/create "創建"
   :left-side-bar/new-page "新頁面"
   :left-side-bar/new-whiteboard "新白板"
   :left-side-bar/nav-favorites "我的最愛"
   :left-side-bar/nav-recent-pages "最近"
   :page/something-went-wrong "出了些問題"
   :page/logseq-is-having-a-problem "Logseq 出了些問題。請按照以下安全步驟將其恢復到正常狀態："
   :page/step "步驟 {1}"
   :page/try "嘗試"
   :page/presentation-mode "簡報模式"
   :page/delete-confirmation "你確定想刪除此頁面檔案嗎？"
   :page/open-in-finder "開啟資料夾"
   :page/open-with-default-app "使用預設應用程式開啟"
   :page/make-public "將其公開讓所有人均可檢視"
   :page/version-history "檢視頁面歷史"
   :page/open-backup-directory "開啟頁面備份資料夾"
   :page/make-private "將其轉為私人"
   :page/delete "刪除頁面"
   :page/add-to-favorites "加入我的最愛"
   :page/unfavorite "從我的最愛移除"
   :page/show-journals "顯示日記頁面"
   :page/show-whiteboards "顯示白板"
   :block/name "頁面名稱"
   :page/earlier "最近"
   :page/copy-page-url "複製頁面連結"
   :file/name "檔案名稱"
   :file/last-modified-at "最後更新於"
   :file/no-data "沒有資料"
   :file/format-not-supported "目前不支援 .{1} 檔案格式"
   :file/validate-existing-file-error "此頁面 {2} 已存在於資料夾 {1}。請選擇其中一個並將您的圖表重新索引。"
   :file-rn/re-index "強烈建議在文件重命名或同步到其他設備後重新索引。"
   :file-rn/need-action "建議按格式重新命名文件。當重新命名的文件同步到其他設備時，需要在所有設備上重新索引。"
   :file-rn/or-select-actions "或者個別重新命名下的文件，然後"
   :file-rn/or-select-actions-2 "。一旦您關閉此面板，這些操作將無法更動。"
   :file-rn/legend "🟢 可選重新命名；🟡 須進行重命名以避免標題更動；🔴 破壞性更動"
   :file-rn/close-panel "關閉控制面板"
   :file-rn/all-action "確認所有操作"
   :file-rn/select-format "（開發者模式專用）選擇文件名稱格式"
   :file-rn/rename "將文件 \"{1}\" 改名為 \"{2}\""
   :file-rn/apply-rename "確定更改文件名稱"
   :file-rn/suggest-rename "需要執行以下動作："
   :file-rn/otherwise-breaking "否則名稱會更改為"
   :file-rn/no-action "完成！不需要額外動作。"
   :file-rn/confirm-proceed "格式更新！"
   :file-rn/select-confirm-proceed "開發者模式：寫作格式"
   :file-rn/unreachable-title "注意！按照目前格式，文件名稱將會變為 {1}，除非有手動設定 `title::` 屬性"
   :file-rn/optional-rename "建議："
   :file-rn/format-deprecated "您目前正在使用一個過時的格式。強烈建議升級到最新的格式。在進行操作之前，請備份您的數據並關閉其他設備上的 Logseq 客戶端。"
   :file-rn/filename-desc-1 "此設定配置了如何將頁面存儲到文件中。Logseq 將一個頁面儲存到一個具有相同名稱的文件中。"
   :file-rn/filename-desc-2 "\"/\" 或 \"?\" 等字元無法成為文件名稱。"
   :file-rn/filename-desc-3 "Logseq 會將無效字元替換為它們的格式以符合連結規則，以使它們變成有效字元。 (e.g. \"?\" => \"%3F\")"
   :file-rn/filename-desc-4 "命名空間分隔符 \"/\" 也會更改為 \"___\" (三個底線) 以便分辨"
   :file-rn/instruct-1 "需要兩個以更新文件名稱："
   :file-rn/instruct-2 "1. 點擊"
   :file-rn/instruct-3 "2. 按照以下說明將文件重命名為新格式："
   :page/created-at "創建於"
   :page/updated-at "更新於"
   :page/backlinks "反向連結"
   :linked-references/filter-search "在已連結頁面搜尋"
   :editor/block-search "正在搜尋區塊"
   :text/image "圖片"
   :asset/show-in-folder "於資料夾顯示圖片"
   :asset/open-in-browser "於瀏覽器開啟圖片"
   :asset/delete "刪除圖片"
   :asset/copy "複製圖片"
   :asset/maximize "圖片最大化"
   :asset/confirm-delete "你確定要刪除 {1} 嗎？"
   :asset/physical-delete "同時刪除原檔案（將無法還原）"
   :color/gray "灰色"
   :color/red "紅色"
   :color/yellow "黃色"
   :color/green "綠色"
   :color/blue "藍色"
   :color/purple "紫色"
   :color/pink "粉紅色"
   :editor/copy "複製"
   :editor/cut "剪下"
   :content/copy-block-ref "複製區塊引用連結"
   :content/copy-block-emebed "複製嵌入區塊"
   :content/open-in-sidebar "在側邊欄中打開"
   :content/click-to-edit "點擊以編輯"
   :settings-page/git-confirm "在更新 Git 設置後，您需要重新啟動應用程式。"
   :settings-page/git-switcher-label "啟用 Git 自動提交"
   :settings-page/git-commit-delay "Git 自動提交間隔"
   :settings-page/edit-config-edn "編輯 config.edn"
   :settings-page/edit-global-config-edn "編輯 global config.edn"
   :settings-page/edit-custom-css "編輯 custom.css"
   :settings-page/edit-export-css "編輯 export.css"
   :settings-page/edit-setting "編輯"
   :settings-page/custom-configuration "個人化設定"
   :settings-page/custom-global-configuration "個人化全域設定"
   :settings-page/custom-theme "個人化主題"
   :settings-page/export-theme "匯出主題"
   :settings-page/show-brackets "顯示括號"
   :settings-page/spell-checker "自動拼音核對"
   :settings-page/auto-updater "自動更新"
   :settings-page/disable-sentry "寄送資料和診斷書給 Logseq"
   :settings-page/disable-sentry-desc "Logseq 永遠不會收集您的圖表資料或出售您的數據。"
   :settings-page/preferred-outdenting "邏輯化縮排"
   :settings-page/show-full-blocks "顯示所有區塊引用列"
   :settings-page/custom-date-format "偏好日期格式"
   :settings-page/custom-date-format-warning "需要重新索引，否則現有的日記引用連結將會失效！"
   :settings-page/preferred-file-format "偏好檔案格式"
   :settings-page/preferred-workflow "偏好工作流"
   :settings-page/preferred-pasting-file "保存連結為文件"
   :settings-page/enable-shortcut-tooltip "啟用快捷鍵提示"
   :settings-page/enable-timetracking "啟用時間追蹤"
   :settings-page/enable-tooltip "啟用提示"
   :settings-page/enable-journals "啟用日記頁面"
   :settings-page/enable-all-pages-public "All pages public when publishing"
   :settings-page/customize-shortcuts "個人化快捷鍵"
   :settings-page/shortcut-settings "設定快捷鍵"
   :settings-page/home-default-page "設定預設首頁"
   :settings-page/enable-block-time "啟用區塊時間截記"
   :settings-page/clear-cache "清除快取資料"
   :settings-page/clear "清除"
   :settings-page/clear-cache-warning "清除快取將捨棄目前的圖表。未儲存的資料將無法復原。"
   :settings-page/developer-mode "開發者模式"
   :settings-page/developer-mode-desc "開發者模式可以協助貢獻者及擴充套件開發人員更有效率地測試 Logseq。"
   :settings-page/current-version "目前版本"
   :settings-page/tab-general "General"
   :settings-page/tab-editor "編輯器"
   :settings-page/tab-version-control "版本控制"
   :settings-page/tab-advanced "進階功能"
   :settings-page/tab-assets "附件設定"
   :settings-page/tab-features "附加功能"
   :settings-page/plugin-system "外掛系統"
   :settings-page/enable-flashcards "啟用卡片"
   :settings-page/network-proxy "網路代理"
   :settings-page/filename-format "文件名稱格式"
   :settings-page/alpha-features "Alpha 功能"
   :settings-page/beta-features "Beta 功能"
   :settings-page/login-prompt "你必須是 Logseq 的 Open Collective Sponsor 或 Backer 以使用新功能。（需要登入）"
   :settings-page/sync "同步"
   :settings-page/enable-whiteboards "啟用白板"
   :yes "是"

   :submit "提交"
   :cancel "取消"
   :close "關閉"
   :delete "刪除"
   :save "儲存"
   :type "種類"
   :host "主持"
   :port "埠"
   :re-index "重新索引"
   :re-index-detail "重新建立索引"
   :re-index-multiple-windows-warning "你需要關閉其他分頁以重新索引"
   :re-index-discard-unsaved-changes-warning "重新索引會捨棄當前的圖表，然後根據目前在磁碟上存儲的所有文件進行重新處理。您將失去未保存的更改，並且這可能需要一些時間。是否繼續？"
   :open-new-window "新分頁"
   :sync-from-local-files "重新整理"
   :sync-from-local-files-detail "從本地文件導入更改"
   :sync-from-local-changes-detected "檢查檔案並更新與目前 Logseq 頁面內容不同的已修改文件。是否繼續？"

   :search/publishing "搜尋"
   :search "搜尋或建立新頁面"
   :whiteboard/link-whiteboard-or-block "連結白板/頁面/區塊"
   :page-search "在目前頁面搜尋"
   :graph-search "搜尋圖表"
   :new-page "新分頁"
   :new-whiteboard "新白板"
   :new-graph "新圖表"
   :graph "圖表"
   :graph/persist "Logseq 正在同步網路資料，請稍候。"
   :graph/persist-error "網路同步狀態異常。"
   :graph/save "正在儲存..."
   :graph/save-success "儲存成功。"
   :graph/save-error "儲存失敗。"
   :graph/all-graphs "所有圖表"
   :graph/local-graphs "本機圖表"
   :graph/remote-graphs "線上圖表"
   :export "匯出"
   :export-graph "匯出圖表"
   :export-page "匯出頁面"
   :export-markdown "以標準 Markdown 匯出（將移除區塊屬性）"
   :export-opml "以 OPML 格式匯出"
   :export-public-pages "匯出公開頁面"
   :export-json "以 JSON 格式匯出"
   :export-roam-json "以 Roam JSON 格式匯出"
   :export-edn "以 EDN 格式匯出"
   :all-graphs "所有圖表"
   :all-pages "所有分頁"
   :all-whiteboards "所有白板"
   :all-files "所有資料"
   :remove-orphaned-pages "移除孤立頁面"
   :all-journals "所有日記"
   :settings "設定"
   :settings-of-plugins "外掛設定"
   :plugins "外掛"
   :themes "主題"
   :relaunch-confirm-to-work "需要重新啟動應用程式才能使其運作。要現在重新啟動嗎？"
   :import "匯入"
   :importing "正在匯入"
   :join-community "加入社群"
   :discourse-title "我們的論壇！"
   :help-shortcut-title "點擊查看快捷鍵和其他提示。"
   :loading "讀取中"
   :parsing-files "爬取資料"
   :loading-files "讀取資料"
   :login "登入"
   :logout "登出"
   :download "下載"
   :language "語言"
   :remove-background "移除背景"
   :remove-heading "移除標頭"
   :heading "標頭 {1}"
   :auto-heading "自動寫入標頭"
   :open-a-directory "於本地開啟資料夾"

   :help/shortcut-page-title "鍵盤快捷鍵"

   :plugin/installed "已下載"
   :plugin/not-installed "尚未下載"
   :plugin/installing "下載中"
   :plugin/install "下載"
   :plugin/reload "重新整理"
   :plugin/update "更新"
   :plugin/check-update "確認更新"
   :plugin/check-all-updates "確認所有更新"
   :plugin/refresh-lists "重整列表"
   :plugin/enabled "已啟用"
   :plugin/disabled "已關閉"
   :plugin/update-available "有可用更新"
   :plugin/updating "正在更新"
   :plugin/uninstall "卸載"
   :plugin/marketplace "市集"
   :plugin/downloads "下載"
   :plugin/stars "星星"
   :plugin/title "標題"
   :plugin/all "所有"
   :plugin/unpacked "未壓縮的"
   :plugin/delete-alert "你確定要卸載外掛 [{1}] 嗎？"
   :plugin/open-settings "開啟設定"
   :plugin/open-package "開啟封包"
   :plugin/load-unpacked "讀取未壓縮的外掛"
   :plugin/restart "重新啟動應用程式"
   :plugin/unpacked-tips "選擇外掛資料夾"
   :plugin/contribute "✨ 貢獻外掛"
   :plugin/up-to-date "已經是最新版本"
   :plugin/custom-js-alert "已找到 custom.js ，確定要執行它嗎？（如果您不了解此檔案的內容，建議不要允許執行，因為這具有一定的安全風險。）"
   :plugin.install-from-file/menu-title "正在從 plugins.edn 下載"
   :plugin.install-from-file/title "正在安裝 plugins.edn 的外掛"
   :plugin.install-from-file/notice "以下的外掛將會取代您的其他外掛："
   :plugin.install-from-file/success "所有外掛均已安裝完成。"

   :pdf/copy-ref "複製引用"
   :pdf/copy-text "複製文字"
   :pdf/linked-ref "已連結的引用"
   :pdf/toggle-dashed "使用線條顯示高亮區塊"
   :pdf/hl-block-colored "使用有色標籤顯示高亮區塊"
   :pdf/doc-metadata "metadata 文件"

   :updater/new-version-install "已下載新版本。"
   :updater/quit-and-install "重新啟動以安裝。"

   :paginates/pages "總共 {1} 頁"
   :paginates/prev "上一頁"
   :paginates/next "下一頁"

   :tips/all-done "全部完成"

   :command-palette/prompt "請輸入指令"
   :select/default-prompt "請選擇提示"
   :select.graph/prompt "請選擇圖表"
   :select.graph/empty-placeholder-description "沒有符合的圖表，您要新增另一個嗎？"
   :select.graph/add-graph "是，新增圖表"

   :file-sync/other-user-graph "目前的圖表正在和其他遠端使用者共用，因此無法同步。"
   :file-sync/graph-deleted "目前的遠端圖表已經被刪除"

   :notification/clear-all "清除所有通知"

   :command.editor/indent                  "縮進塊標簽"
   :command.editor/outdent                 "取消縮進塊"
   :command.editor/move-block-up           "向上移動塊"
   :command.editor/move-block-down         "向下移動塊"
   :command.editor/new-block               "創建塊"
   :command.editor/new-line                "塊中新建行"
   :command.editor/zoom-in                 "聚焦"
   :command.editor/zoom-out                "推出聚焦"
   :command.editor/follow-link             "跟隨光標下的鏈接"
   :command.editor/open-link-in-sidebar    "在側邊欄打開"
   :command.editor/expand-block-children   "展開"
   :command.editor/collapse-block-children "折疊"
   :command.editor/select-block-up         "選擇上方的塊"
   :command.editor/select-block-down       "選擇下方的塊"
   :command.editor/select-all-blocks       "選擇所有塊"
   :command.ui/toggle-help                 "顯示/關閉幫助"
   :command.git/commit                     "提交消息"
   :command.go/search                      "全文搜索"
   :command.ui/toggle-document-mode        "切換文檔模式"
   :command.ui/toggle-theme                "“在暗色/亮色主題之間切換”"
   :command.ui/toggle-right-sidebar        "啟用/關閉右側欄"
   :command.go/journals                    "跳轉到日記"})