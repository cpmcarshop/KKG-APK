# APKを作る方法

このプロジェクトにはGitHub Actionsの自動ビルド設定を含めています。

1. このフォルダをGitHubリポジトリへアップロード
2. GitHubの「Actions」を開く
3. 「Build APK」を選ぶ
4. 「Run workflow」を押す
5. 完了後、実行結果のArtifactsから `CPMCarShop-debug-apk` を取得

Android Studioを使う場合はプロジェクトを開いて `app:assembleDebug` を実行してください。
