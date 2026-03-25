# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## アプリ概要

Nintendo Switchの画像転送機能を使いやすくするAndroidアプリ。

**転送フロー:**
1. SwitchがQRコードでWi-Fi接続情報を提示
2. アプリがQRコードを読み取る（`WIFI:T:WPA;S:<SSID>;P:<pass>;;` 形式）
3. アプリが自動でそのWi-Fiホットスポットに接続
4. SwitchのHTTPサーバーから画像・動画を取得・保存（今後実装）

## Build Commands

```bash
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew test                   # ユニットテスト
./gradlew connectedAndroidTest   # 実機/エミュレータテスト
```

## Architecture

**パッケージ:** `space.aioilight.quicknsscrap`

| パッケージ | 役割 |
|-----------|------|
| `qr/` | `WifiQrParser` — WIFI QRコード文字列を `WifiCredentials` にパース |
| `wifi/` | `WifiConnector` — `WifiNetworkSpecifier` + `ConnectivityManager.requestNetwork()` でWi-Fi接続 |
| `transfer/` | `SwitchClient` — `network.openConnection()` でSwitch経由にバインドしてHTTP通信、`SwitchMediaFile` データクラス |
| `ui/screen/` | `QrScannerScreen` — CameraX + ML Kit QRスキャン、`TransferScreen` — ファイル一覧表示・ダウンロード |
| `MainActivity.kt` | `AppState` シールドクラスで画面遷移を管理（Scanning → Connecting → Connected → Transfer） |

**Wi-Fi接続の注意:** `WifiNetworkSpecifier` はP2P接続APIのためAndroidの「インターネットなし」通知が出ない。接続後は `bindProcessToNetwork(network)` でプロセスをSwitchのネットワークに向ける（`release()` 時に `null` で解除）。HTTP通信は通常の `URL().openConnection()` で可。

**data.json 実際のフォーマット:**
```json
{ "FileType": "photo", "ConsoleName": "すいっち", "FileNames": ["2024XXXX.jpg", ...] }
```
- `FileType`: `"photo"` or `"movie"` — バッチ単位で型が決まる（ファイルリスト全体が同じ型）
- 画像URL: `http://192.168.0.1/img/{filename}`（`SwitchClient.kt` の `IMG_BASE`）

**ファイル保存先:** MediaStore経由でギャラリーに保存（画像: `Pictures/QuickNSScrap`、動画: `Movies/QuickNSScrap`）。API 29+なので `WRITE_EXTERNAL_STORAGE` 権限不要。

## Tech Stack

| 項目 | 値 |
|------|-----|
| Kotlin | 2.0.21 |
| AGP | 8.13.2 |
| Compile SDK | 36 / Min SDK | 34 |
| Compose BOM | 2024.09.00 |
| CameraX | 1.4.1 |
| ML Kit Barcode | 17.3.0 |
| Java compatibility | 11 |
