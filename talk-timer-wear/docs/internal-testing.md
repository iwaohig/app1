# 内部テストの手順（Google Play）

Play ストア経由で、登録したテスター（最大 100 人）だけにアプリを配る手順です。
Play Console の画面の名前や場所は変わることがあるので、見つからない場合は Play Console のヘルプで探してください。

## 1. アップロード鍵を作る（最初の1回だけ・自分の PC で）

リポジトリの**外**に鍵を作ります。鍵とパスワードはなくさないよう、パスワード管理ツールなどにバックアップしてください。

```bash
mkdir -p ~/keys
keytool -genkeypair -v -keystore ~/keys/moonlightfield-upload.jks -alias talktimer \
  -keyalg RSA -keysize 4096 -validity 10000
```

- 名前や組織を聞かれたら、例えば `CN=GEKKOHARA GIKEN, O=Moonlightfield, C=JP` のように入力します
- この鍵は「アップロード鍵」です。ストアで配る本当の署名は Google が管理します（Play App Signing）。そのため、アップロード鍵をなくしても Google に申請すればリセットできますが、手間がかかるので保管は確実に

## 2. 鍵の設定ファイルを作る

`talk-timer-wear/keystore.properties.example` をコピーして `talk-timer-wear/keystore.properties` を作り、中身を書き換えます（このファイルは Git に入りません）。

```properties
storeFile=/Users/<あなた>/keys/moonlightfield-upload.jks
storePassword=<鍵ストアのパスワード>
keyAlias=talktimer
keyPassword=<鍵のパスワード>
```

## 3. リリース用のファイル（AAB）を作る

```bash
cd talk-timer-wear
./gradlew bundleRelease
```

`app/build/outputs/bundle/release/app-release.aab` ができます（約 3 MB）。

**2回目以降のアップロードでは**、`app/build.gradle.kts` の `versionCode` を 1 ずつ増やしてからビルドしてください（同じ番号は再アップロードできません）。`versionName`（表示用の番号、例 `0.1.1`）も合わせて上げます。

- 実例：2026-11-08 に versionCode 2 / `0.2.0`（設定画面・終了操作・残り1分でアプリが落ちる不具合の修正）を同じ内部テストのトラックに公開しました

## 4. Play Console でアプリを作る（最初の1回だけ）

1. Moonlightfield のアカウントで Play Console を開き、「アプリを作成」
   - アプリ名：プレゼンタイマー
   - デフォルトの言語：日本語 – ja-JP
   - アプリ／ゲーム：アプリ　無料／有料：無料
   - パッケージ名：`dev.moonlightfield.talktimer` を入力して「使用できるか確認する」を押す。**ここでパッケージ名が固定され、あとから変えられません**
   - 「自動保護」は既定でオン（今回はオンのまま作成）
2. Wear OS のフォームファクタを追加する（設定の「詳細設定」→「フォームファクタ」付近）
   - 時計専用アプリなので、Wear OS 用のリリーストラックを使います
   - 作成直後のフォームファクタには、Android XR が最初から有効な状態で入っています

## 5. 内部テストのリリースを作る

1. 「テスト」→「内部テスト」（Wear OS 用のトラック）を開く
2. 「テスター」タブでメーリングリストを作り、テスターの Google アカウントのメールアドレスを登録
   - テスターは、時計で使っている Google アカウントで登録してください
3. 「新しいリリースを作成」
   - Play App Signing は選ぶ画面が出ず、最初から Google が生成した署名鍵が使われます。アプリ署名鍵の SHA-256 は次のとおり
     `E1:CA:D4:0D:C6:75:A7:FF:95:F0:4D:87:B0:35:99:0B:A4:F8:E5:49:F1:36:D2:B8:07:E9:B0:93:94:4A:13:DD`
   - `app-release.aab` をアップロード
   - プレビューで「ネイティブ コードのデバッグ シンボルがアップロードされていません」という警告が出ますが、そのまま公開できます
   - リリースノートを書く（例：「初回テスト版」）
4. 保存して、内部テストとして公開
   - 入力が必要な項目（プライバシーポリシーの URL など）があれば案内が出るので、`docs/release-checklist.md` を見ながら入力します

## 6. テスターがインストールする

1. 「テスター」タブにある**参加用リンク**をテスターに送る
   - プレゼンタイマーの参加用リンク：https://play.google.com/apps/internaltest/4699326998712789374
2. テスターはリンクを開き、テストに参加する
3. 時計の Play ストアで「プレゼンタイマー」を探してインストール
   - スマホの Play ストアのアプリページから、「Install on more devices」で時計を選んでインストールすることもできます
   - 初回公開のあと数時間は、ストアで「アイテムは見つかりませんでした」と出て、Play Console のトラックも「非アクティブ」の表示のままでした。待つとストアにページが出て、「Install on more devices」から時計に入れられました

## 7. 試すこと

README の「Pixel Watch に入れる」のあとにある確認項目と同じです。特に、**手首を下ろして画面が消えている間も、振動が時間どおりに鳴るか**を確認してください。2分の持ち時間なら、残り1分・時間切れ・1分超過の3回の振動をすぐ試せます。

## 補足

- 内部テストは、クローズドテストや本番より審査が軽く、すぐ配れます
- Wear OS として本番公開する前には、別途クローズドテストが必要です（`docs/release-checklist.md`）
- CI（GitHub Actions など）でビルドする場合は、`keystore.properties` の代わりに環境変数 `TALKTIMER_KEYSTORE`・`TALKTIMER_KEYSTORE_PASSWORD`・`TALKTIMER_KEY_ALIAS`・`TALKTIMER_KEY_PASSWORD` でも設定できます
