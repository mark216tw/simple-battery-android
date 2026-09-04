# 簡單電池 Android APP Session 整理

## 專案概要

- APP 名稱：簡單電池
- Application ID：`com.simplebattery.app`
- 開發語言：Kotlin
- APP UI：Jetpack Compose + Material 3
- 桌面小工具：`AppWidgetProvider` + `RemoteViews`
- 最低支援版本：Android 8.0（API 26）
- `compileSdk`：35
- `targetSdk`：35

## 產品需求

### APP 主畫面

- 顯示目前電池百分比。
- 顯示充電中、已充滿或使用電池中的狀態。
- 顯示電源類型、健康狀態、溫度與電壓。
- 介面簡單、易用，並提供足夠大的操作區域。
- 主畫面的設定入口使用齒輪圖示。

### APP 外觀設定

- 提供以下模式：
  - 跟隨系統，預設值。
  - 淺色模式。
  - 深色模式。
- 提供單一色相彩色滑桿，自訂 APP 主題色。
- 主題模式與主題色變更後立即套用。
- 狀態列及 Navigation Bar 圖示會依介面明暗切換。
- 已選項使用實心主色背景、高對比文字、粗體與勾選圖示。

### 桌面小工具

- 初始大小為 1×1。
- 可由 Launcher 水平及垂直調整大小。
- 顯示目前電量，例如 `85%`。
- Launcher 提供手機電量、電池溫度與藍牙裝置電量三個獨立小工具。
- 外框可選無外框、圓形進度環或直式電池。
- 直式電池上方有正極凸點，內部依數值由下往上填滿。
- 圓形外框從正上方開始，依數值順時針顯示進度。
- 充電時在電量數字上方顯示小型閃電，未充電時顯示小型電池。
- 調整為較寬尺寸時，直式電池保持比例並置中，不會拉成橫向。
- 電池外框會配合空間放大，文字字級仍由使用者設定。
- 空間不足時會自動縮小文字，避免 `100%` 被裁切。

### 小工具設定

- APP 與小工具的主題色分開設定。
- 每個小工具實例可擁有獨立設定。
- 提供色相彩色滑桿。
- 提供 `28–52sp` 連續字級滑桿。
- 提供以下背景樣式：
  - 主題色背景：整個小工具使用圓角主題色底板。
  - 透明背景：不繪製底板。
- 手機及藍牙電量可設定是否顯示 `%`。
- 藍牙電量可設定是否顯示裝置名稱。
- 小工具主題色採較高飽和度與亮度。
- 透明背景下，數字及閃電使用主題色與黑白對比描邊。
- 主題色背景下，數字及閃電自動選擇黑色或白色高對比色。
- 新增小工具時開啟設定頁。
- 長按既有小工具後，可透過支援的 Launcher 原生編輯入口再次進入該實例的設定頁。
- 完成設定後回到桌面；「返回」按鈕開啟 APP 主畫面。
- 小工具設定使用獨立 task，避免之後點擊 APP 圖示進入設定頁。
- 設定頁採較緊湊排版，並提供即時預覽。
- 設定預覽與實際小工具共用相同繪圖邏輯。
- 點擊手機電量或溫度小工具會直接開啟 Android 電池設定，不經 APP 主畫面。
- 點擊藍牙電量小工具會直接開啟 Android 藍牙設定，不經 APP 主畫面。
- 支援的 Launcher 長按小工具後會提供原生編輯圖示，進入樣式設定。
- APP 圖示提供「電池」快捷選單，可直接開啟 Android 電池設定。

## 小工具更新策略

- 使用 WorkManager 約每 15 分鐘更新一次。
- 接收到充電接入、充電拔除、低電量及電量恢復事件時更新。
- 系統明暗設定變更時重新繪製。
- APP 在前景收到 `ACTION_BATTERY_CHANGED` 時同步更新小工具。
- 不使用常駐前景服務，避免因顯示電量而增加耗電。
- Android 背景排程不保證精準到分鐘。

## 新增小工具

### 電池溫度

- 顯示溫度數字與 `°C`，上方顯示溫度計圖示。
- 圓形及電池填滿比例採 `0–50°C`。
- 40°C 以上改用警示色。
- 無法取得資料時顯示 `--°C`。

### 藍牙裝置電量

- 設定時選擇一個已配對裝置。
- Android 12+ 使用 `BLUETOOTH_CONNECT` 執行階段權限。
- 僅透過公開 GATT API 讀取標準 BLE Battery Service。
- 只讀取目前已透過支援 profile 連線的裝置，不主動喚醒離線裝置。
- 連線判定同時檢查 A2DP、HFP、GATT、LE Audio 與助聽器 profile。
- 監聽系統耳機電量變更事件並依裝置位址快取最近數值。
- 已連線但尚未收到系統數值時顯示「等待電量」，重新連線耳機可觸發更新。
- 離線、不支援、未設定或缺少權限時顯示 `--` 與原因。
- 部分經典藍牙或使用私有協定的耳機無法提供電量。

## 充電資料

- 主畫面充電時顯示即時電流、估算功率及預估充滿時間。
- 充電資料區塊顯示在一般電源資料區塊上方。
- 裝置未提供對應資料時顯示「裝置不支援」。
- 主畫面與設定畫面在每次恢復前景時重新讀取電池資料。

## 小工具渲染方式

小工具使用 Bitmap 繪製後交給 RemoteViews 顯示，主要原因如下：

- 能精確繪製直式電池外框與正極。
- 能繪製一致的小型 Path 閃電，不依賴 Emoji 字型。
- 能為透明背景的文字及閃電加入真正的描邊。
- 能依 Launcher 提供的實際尺寸重新計算版面。
- 能限制大型小工具的 Bitmap 解析度，降低記憶體及 RemoteViews 傳輸風險。

電池外型會維持直式比例。較寬的小工具會保留透明空間；較高的小工具可使用更修長的直式電池形狀。

## APP 圖示

- 核心圖案為閃電。
- 採活潑、友善、粗線條卡通工具風格。
- Adaptive Icon 畫布為 `108 × 108dp`。
- 外框裁切參考區為 `72 × 72dp`。
- 安全區為 `66 × 66dp`。
- 閃電核心高度約 60dp，接近安全區的 90%。
- 提供一般 Adaptive Icon、圓形圖示及 Android 13+ monochrome 圖示。

## 設定資料

APP 設定使用 Preferences DataStore：

```text
theme_mode
theme_hue
```

小工具設定依 `appWidgetId` 儲存：

```text
widget_<id>_hue
widget_<id>_font_size
widget_<id>_background
widget_<id>_frame
widget_<id>_type
widget_<id>_show_percent
widget_<id>_bluetooth_address
widget_<id>_show_device_name
```

小工具被刪除時會清除對應設定。

## 主要檔案

- `app/src/main/java/com/simplebattery/app/MainActivity.kt`
  - APP 主畫面、即時電池狀態及 APP 設定頁。
- `app/src/main/java/com/simplebattery/app/SystemSettingsIntents.kt`
  - 解析並直接啟動 Android 電池或藍牙系統設定元件。
- `app/src/main/java/com/simplebattery/app/data/BatteryInfo.kt`
  - 電池廣播資料解析及百分比換算。
- `app/src/main/java/com/simplebattery/app/data/AppSettingsRepository.kt`
  - APP 主題模式與色相設定。
- `app/src/main/java/com/simplebattery/app/ui/theme/SimpleBatteryTheme.kt`
  - Material 3 色彩、明暗模式及系統列外觀。
- `app/src/main/java/com/simplebattery/app/ui/HueSlider.kt`
  - APP 與小工具共用的色相滑桿。
- `app/src/main/java/com/simplebattery/app/widget/BatteryWidgetProvider.kt`
  - 小工具生命週期、點擊事件與更新。
- `app/src/main/java/com/simplebattery/app/widget/WidgetRenderer.kt`
  - 直式電池、百分比、閃電與描邊 Bitmap 渲染。
- `app/src/main/java/com/simplebattery/app/widget/WidgetConfigActivity.kt`
  - 小工具個別設定與預覽。
- `app/src/main/java/com/simplebattery/app/widget/WidgetPreferences.kt`
  - 每個小工具實例的設定儲存。
- `app/src/main/java/com/simplebattery/app/widget/WidgetUpdateWorker.kt`
  - 15 分鐘定期更新工作。
- `app/src/main/java/com/simplebattery/app/widget/PackageUpdateReceiver.kt`
  - APP 更新後重新整理既有小工具的點擊行為與畫面。
- `app/src/main/java/com/simplebattery/app/widget/TemperatureWidgetProvider.kt`
  - 電池溫度小工具生命週期與更新。
- `app/src/main/java/com/simplebattery/app/widget/BluetoothBatteryWidgetProvider.kt`
  - 藍牙裝置電量小工具生命週期與更新。
- `app/src/main/java/com/simplebattery/app/widget/BluetoothBatteryReader.kt`
  - 已配對裝置清單、權限檢查及標準 BLE Battery Service 讀取。
- `app/src/main/res/xml/battery_widget_info.xml`
  - 小工具初始尺寸、可調整方向及 Provider 設定。

## 驗證結果

已完成以下驗證：

- `./gradlew testDebugUnitTest`：通過。
- `./gradlew lintDebug`：通過。
- `./gradlew assembleDebug`：通過。
- `./gradlew assemblePrerelease`：通過，包含 R8 與資源壓縮。
- 電量比例換算包含 3 項單元測試。

Debug APK：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 尚待裝置驗證

開發期間沒有連接模擬器或 Android 實機，因此仍建議在裝置上確認：

- Android 8、Android 12 及最新 Android 版本的畫面。
- 不同品牌 Launcher 的 1×1 初始尺寸。
- 1×1、1×2、2×1、2×2 及更大尺寸的直式電池比例。
- `9%`、`85%`、`100%` 與字級上下限。
- 充電接入、拔除及充滿後閃電的顯示狀態。
- 深色、淺色及透明背景在不同桌布上的可讀性。
- 狀態列與 Navigation Bar 在三種 APP 外觀模式下的圖示對比。

## 發行建置

- `prerelease` Build Type 使用 Debug 金鑰簽署。
- `prerelease` 啟用 R8 程式碼壓縮及資源壓縮。
- 版本名稱為 `1.0.0-prerelease.2`，僅供測試。
- 公開專案文件包含 README、使用指南、建置文件、隱私說明、安全性政策、版本紀錄及 MIT License。
