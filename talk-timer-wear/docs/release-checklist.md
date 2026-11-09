# Google Play 公開チェックリスト（プレゼンタイマー）

アカウント：既存の組織アカウント（デベロッパー名 Moonlightfield / 法的な名前 GEKKOHARA GIKEN）
パッケージ名：`dev.moonlightfield.talktimer`　価格：無料・広告なし

## 1. 公開前に用意するもの

- [x] **プライバシーポリシーを公開する**
  `docs/privacy/index.html` を `https://moonlightfield.dev/apps/talktimer/privacy` に置く（4言語を1ページに収録）。公開済み。
  アプリ内のリンク先（`Links.PRIVACY_POLICY_URL`）と同じ URL にすること。
- [x] **アップロード鍵を作り、安全に保管する**（将来スマホ版と連動するときも同じ鍵を使う）
  ```bash
  keytool -genkeypair -v -keystore moonlightfield-upload.jks -alias talktimer \
    -keyalg RSA -keysize 4096 -validity 10000
  ```
  鍵ファイルとパスワードはリポジトリに入れない。Play App Signing を有効にする。
- [x] **リリース用 AAB を作る**：`keystore.properties` を用意して `./gradlew bundleRelease`（手順は `docs/internal-testing.md`）
- [x] **スクリーンショット**：Wear OS 用に1枚以上、1:1・384×384 px 以上、端末の枠や画面にない文字を入れない。タイルの画面も載せる（推奨）。Android Studio の Wear OS エミュレータで「Play Store Compatible」を選んで撮影する
  `fastlane/metadata/android/ja-JP/images/wearScreenshots/` に5枚あり。
- [x] **アイコン**：512×512 px（32ビット PNG）
  `fastlane/metadata/android/ja-JP/images/icon.png`
- [x] **フィーチャー グラフィック**：1024×500 px
  `fastlane/metadata/android/ja-JP/images/featureGraphic.png`（画像は `tools/make_store_images.ps1` で作り直せる）

## 2. Play Console の入力内容

### ストアの掲載情報
`fastlane/metadata/android/<言語>/` に4言語分あります（title 30字・short 80字・full 4000字以内を確認済み）。

| 言語 | フォルダ |
|---|---|
| 日本語 | `ja-JP` |
| 英語（米国） | `en-US` |
| 韓国語 | `ko-KR` |
| 中国語（簡体字） | `zh-CN` |

### データ セーフティ
- ユーザーデータを収集または共有しますか → **いいえ**
- （データは端末内にのみ保存され、外部に送信されない。外部 SDK なし）

### 正確なアラームの権限（USE_EXACT_ALARM）の申告
- Play Console では「**目覚まし時計**」の用途を選んで保存済み
- 審査で却下された場合は、`USE_EXACT_ALARM` をやめて `SCHEDULE_EXACT_ALARM`（ユーザーが許可する方式）に切り替える方針
- 説明文の例：
  > ユーザーが開始したプレゼン用カウントダウンタイマーで、持ち時間の半分・残り10/5/3/1分・時間切れの時刻に振動で通知します。画面が消えていても正確な時刻に振動させる必要があるため、正確なアラームを使用しています。
  >
  > The app is a countdown timer started by the user. It vibrates at halfway, at 10/5/3/1 minutes left and at time up, and must fire at the exact time even when the screen is off.

### そのほか
- [ ] コンテンツのレーティング（アンケート）
- [ ] ターゲット ユーザー（年齢層）
- [ ] アプリのカテゴリ：仕事効率化
- [ ] 連絡先メール：contact@moonlightfield.dev

## 3. Wear OS としての公開手順
1. Play Console で Wear OS のフォームファクタを追加
2. クローズドテストを実施（Wear OS の公開前に必要）
3. Wear OS へのオプトインと審査

## 4. 品質要件の自己チェック
- [x] 時計単体で動作（`com.google.android.wearable.standalone=true`）
- [x] ターゲット API 35 以上（現在 36）
- [x] 実行中は Ongoing Activity を使用
- [x] 背景は黒
- [x] タイルにプレビュー画像あり
- [x] 丸い画面で文字が切れないか（Pixel Watch 3 で確認済み）
- [ ] スワイプで画面を閉じられるか（実機で確認。未確認）
- [x] 常時表示モードの見た目（Pixel Watch 3 で確認済み）
