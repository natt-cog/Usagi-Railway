#!/bin/sh
# デモ環境リセット
#   使い方: demo/reset.sh [タグ]   (既定: demo/0-legacy)
#   - 8080 で動いている URMS を停止
#   - 作業ブランチ demo/live をタグの状態で作り直す (main やほかのブランチは変更しない)
#   - バッチの work/ と target/ を削除
#   - --start を付けると spring-boot:run で起動 (ログ: /tmp/urms.log)
set -e
cd "$(dirname "$0")/.."
TAG="demo/0-legacy"
START=0
for a in "$@"; do
  case "$a" in
    --start) START=1 ;;
    *) TAG="$a" ;;
  esac
done

if ! git diff --quiet || ! git diff --cached --quiet; then
  echo "未コミットの変更があります. git stash などで退避してから実行してください." >&2
  exit 1
fi

PID=$(lsof -t -iTCP:8080 -sTCP:LISTEN 2>/dev/null || true)
if [ -n "$PID" ]; then
  echo "URMS 停止 (pid $PID)"
  kill $PID
  sleep 2
fi

git fetch --tags --quiet origin 2>/dev/null || true
git rev-parse -q --verify "refs/tags/$TAG" >/dev/null || { echo "タグ $TAG がありません" >&2; exit 1; }
git checkout -q -B demo/live "$TAG"
rm -rf batch/c/work batch/java/work target
echo "demo/live を $TAG ($(git rev-parse --short HEAD)) にリセットしました"

if [ "$START" = 1 ]; then
  JAVA_HOME=${JAVA_HOME:-/usr/lib/jvm/java-8-openjdk-amd64} nohup mvn -B -q spring-boot:run > /tmp/urms.log 2>&1 &
  printf "起動待ち"
  i=0
  until grep -q "Started UsagiRailwayApplication" /tmp/urms.log 2>/dev/null; do
    i=$((i + 1)); [ $i -gt 90 ] && { echo " タイムアウト (/tmp/urms.log を確認)"; exit 1; }
    printf "."; sleep 2
  done
  echo " http://localhost:8080/urms/ (shirei / shirei123)"
fi
