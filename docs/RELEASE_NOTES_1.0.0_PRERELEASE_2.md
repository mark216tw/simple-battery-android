# 簡單電池 1.0.0-prerelease.2

這是使用 Debug 金鑰簽署的 Pre-release 測試版本，不是正式發行版本，請勿用於正式上架或生產環境。

## 本次更新

- 點擊手機電量或電池溫度小工具，可直接開啟 Android 電池設定。
- 點擊藍牙裝置電量小工具，可直接開啟 Android 藍牙設定。
- 長按 APP 圖示新增「電池」快捷選單，可直接開啟 Android 電池設定。
- 支援的 Launcher 長按小工具後會提供原生編輯入口，可重新調整小工具樣式。
- 系統設定入口不再經過 APP 中繼 Activity，避免短暫顯示 APP 啟動畫面。
- APP 更新後會自動重新整理既有小工具的點擊行為。

## 已知限制

- 小工具原生編輯入口由 Launcher 提供，部分 Launcher 可能不顯示編輯按鈕。
- 各品牌 Android 裝置的電池設定頁面與名稱可能不同。
- 部分手機不提供充電電流或預估時間。
- Android 沒有支援所有耳機的公開電量 API；部分耳機可能顯示「等待電量」或「不支援」。
- 背景更新可能受 Android 省電政策延後。
- Debug 簽章與未來正式版本簽章不同，正式版可能需要先移除此測試版。

## 建置資訊

- Build Type：`prerelease`
- Version Code：`2`
- Version Name：`1.0.0-prerelease.2`
- R8：已啟用
- 資源壓縮：已啟用
- 簽署：Android Debug keystore
- Debuggable：否，確保 R8 完整最佳化與混淆
