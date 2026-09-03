# 開發與建置文件

## 環境需求

- Android Studio 最新穩定版本。
- JDK 17。
- Android SDK Platform 35。
- Android SDK Build Tools 35.x。
- Git。

專案已包含 Gradle Wrapper 8.11.1，不需要另外安裝 Gradle。

## 取得原始碼

```bash
git clone https://github.com/mark216tw/simple-battery-android.git
cd simple-battery-android
```

## 建置 Debug 版本

Windows：

```powershell
.\gradlew.bat assembleDebug
```

macOS 或 Linux：

```bash
./gradlew assembleDebug
```

輸出位置：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 建置 Pre-release 測試版

Windows：

```powershell
.\gradlew.bat assemblePrerelease
```

macOS 或 Linux：

```bash
./gradlew assemblePrerelease
```

`prerelease` Build Type 具有以下特性：

- 版本名稱：`1.0.0-prerelease.1`。
- Application ID：`com.simplebattery.app.prerelease`。
- 使用本機自動產生的 Android Debug keystore 簽署。
- `debuggable=false`，確保 R8 完整執行最佳化與混淆。
- 啟用 R8 程式碼壓縮、最佳化與混淆。
- 啟用 Android 資源壓縮。
- 僅供測試，不得作為正式上架簽署版本。

原始輸出位置：

```text
app/build/outputs/apk/prerelease/app-prerelease.apk
```

## 測試與靜態檢查

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat lintPrerelease
```

## 正式發行注意事項

- 建立並妥善保管正式 release keystore。
- 不得使用 Debug 金鑰發布正式版本。
- 更新 `versionCode` 與 `versionName`。
- 檢查 R8 mapping、功能測試與各 Android 版本相容性。
- 在實機驗證 Launcher 小工具、充電資料與藍牙裝置。
- 正式上架前再次檢查隱私政策與商店資料安全聲明。
