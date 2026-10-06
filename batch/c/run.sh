#!/bin/sh
# URPWD01 電力日報バッチ 実行スクリプト
#   使い方: ./run.sh [TLM_YYYYMMDD.DAT へのパス]
#   出力  : work/DAILY_YYYYMMDD.DAT
set -e
cd "$(dirname "$0")"
INPUT="${1:-data/TLM_20261005.DAT}"
make -s
set +e
./work/urpwd01 "$INPUT" work
RC=$?
set -e
echo "RC=$RC"
[ $RC -ge 8 ] && exit $RC
OUT=work/DAILY_$(basename "$INPUT" | sed 's/^TLM_//')
EXP=expected/$(basename "$OUT")
if [ -f "$EXP" ]; then
  if diff -q "$OUT" "$EXP" >/dev/null; then
    echo "GOLDEN OK: $(basename "$OUT") matches $EXP"
  else
    echo "GOLDEN MISMATCH:"; diff "$OUT" "$EXP" || true; exit 1
  fi
fi
exit 0
