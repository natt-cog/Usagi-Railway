#!/bin/sh
# URINS01 検査期限算出バッチ 実行スクリプト (GnuCOBOL 3.x)
#   使い方: ./run.sh [FORMATIONS.DAT へのパス]
#   出力  : work/INSPDUE.DAT
set -e
cd "$(dirname "$0")"
INPUT="${1:-data/FORMATIONS.DAT}"
mkdir -p work
cobc -x -o work/urins01 URINS01.cbl
cp "$INPUT" work/FORMATIONS.DAT
cd work
set +e
./urins01
RC=$?
set -e
echo "RC=$RC"
[ $RC -ge 8 ] && exit $RC
if [ -f ../expected/INSPDUE.DAT ]; then
  if diff -q INSPDUE.DAT ../expected/INSPDUE.DAT >/dev/null; then
    echo "GOLDEN OK: INSPDUE.DAT matches expected/INSPDUE.DAT"
  else
    echo "GOLDEN MISMATCH:"; diff INSPDUE.DAT ../expected/INSPDUE.DAT || true; exit 1
  fi
fi
exit 0
