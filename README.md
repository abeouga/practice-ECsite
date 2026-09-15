# 通販システム MVP

Spring Boot REST API と React + TypeScript の学習用MVPです。画面とAPIの翻訳方針は [docs/design.md](docs/design.md) に記載しています。

## 必要な環境

- Java 21以上（`pom.xml`のコンパイル設定はJava 21）
- Maven 3.9以上
- Node.js 24以上、npm

## バックエンドの構成

```text
backend/src/main/java/com/example/backend/
├─ controller/  # HTTPリクエストとレスポンス
├─ service/     # 購入・カートなどの処理
├─ repository/  # データベースアクセス
├─ entity/      # テーブルに対応するクラス
├─ dto/         # APIの入力・出力データ
├─ exception/   # 例外とエラーレスポンス
└─ config/      # Security・初期データなどの設定
```

## 起動

Windowsでは、プロジェクトフォルダ内の `start-ecshop.bat` をダブルクリックしてください。Java、Node.js、npmを確認し、必要ならフロントエンド依存関係をインストールした後、バックエンドとフロントエンドを起動してブラウザを開きます。プロジェクトフォルダを別PCへコピーした場合も、同じバッチを実行できます。

ショートカットを作成する場合は、同じフォルダの `create-shortcut.bat` を実行してください。ショートカット自体はPCごとのパスを持つため、別PCへコピーした後に再生成してください。

ターミナルを2つ使用します。

```powershell
# ターミナル1
Set-Location backend
mvn spring-boot:run
```

```powershell
# ターミナル2
Set-Location frontend
npm install
npm run dev
```

ブラウザで http://localhost:5173 を開きます。フロントエンドの `/api` は http://localhost:8080 へプロキシされます。

## 開発用ログイン情報

- `alice@example.com` / `password`
- `bob@example.com` / `password`

初回起動時に2ユーザー、食品・文具・生活用品の複数商品、在庫0の商品を登録します。通常起動のDBは `backend/data/ecshop.mv.db` のH2ファイルDBで、再起動後も操作結果を保持します。

## テスト

```powershell
Set-Location backend
mvn test

Set-Location ..\frontend
npm run build
npm run lint
```

バックエンド統合テストは、認証、CSRF、商品一覧・ジャンル、在庫0拒否、利用者間のカート／履歴分離、複数商品の購入、購入後のカート削除、空カートへの再購入拒否を確認します。フロントエンドはTypeScriptコンパイルを含む本番ビルドとLintを実行します。

## 実装範囲と制限

- HTTPセッション + BCrypt + CSRF Cookieを使用します。JWTは使用しません。
- 購入確定は1トランザクションで購入記録保存と対象カート削除を行います。同一利用者の購入は利用者行の悲観ロックで直列化します。
- 購入価格は確定時の商品単価を保存します。価格計算はlong整数とオーバーフロー検出を使用します。
- 購入時の在庫減算・予約・数量上限、カート編集・個別削除、決済・配送、会員登録、商品管理は実装していません。
- 設計画像ファイルは指定添付フォルダで確認できなかったため、画像固有の未記載仕様は追加していません。
