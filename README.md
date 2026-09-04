# 簡單電池

「簡單電池」是一款以簡潔、易讀與低耗電為目標的 Android 電池狀態 APP。主畫面提供目前電量與充電資訊，並包含手機電量、電池溫度及相容藍牙裝置電量三種桌面小工具。

## 功能特色

- 即時顯示手機電量、充電狀態、健康狀態、溫度與電壓。
- 充電時顯示即時電流、估算功率與預估充滿時間。
- 支援跟隨系統、淺色及深色模式。
- APP 與各個小工具可分別自訂主題色。
- 桌面小工具可選無外框、圓形進度環或直式電池外框。
- 支援透明或主題色背景、連續字級及百分比符號開關。
- 提供手機電量、電池溫度與藍牙裝置電量小工具。
- 點擊電量或溫度小工具可直接開啟 Android 電池設定，點擊藍牙小工具可直接開啟藍牙設定，不經 APP 主畫面。
- 長按 APP 圖示可使用「電池」快捷選單開啟 Android 電池設定。
- 支援 Android 8.0（API 26）以上版本。

## 螢幕與權限

APP 沒有 `INTERNET` 權限，不會透過網路傳送資料。藍牙電量小工具在 Android 12 以上需要「附近裝置」權限，僅用於列出已配對裝置及讀取相容裝置的標準 BLE Battery Service。

部分藍牙耳機使用 Android 系統內部或廠商私有協定回報電量，第三方 APP 不一定能取得。詳細限制請參閱[使用指南](docs/USAGE.md)。

## 安裝測試版本

1. 前往 GitHub Releases。
2. 下載 `app-prerelease.apk`。
3. 在 Android 裝置允許該來源安裝未知 APP。
4. 安裝 APK 並開啟「簡單電池」。

此 APK 是 Pre-release 測試版，使用 Debug 金鑰簽署，不是正式發行版本。

## 文件

- [使用指南](docs/USAGE.md)
- [開發與建置](docs/BUILDING.md)
- [隱私說明](PRIVACY.md)
- [安全性政策](SECURITY.md)
- [版本紀錄](CHANGELOG.md)
- [Session 整理](SESSION_SUMMARY.md)

## 技術架構

- Kotlin
- Jetpack Compose + Material 3
- Preferences DataStore
- `AppWidgetProvider` + `RemoteViews`
- WorkManager
- Bluetooth GATT Battery Service

## 授權

本專案使用 [MIT License](LICENSE) 授權。
