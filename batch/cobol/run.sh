#!/bin/sh
# URINS01 検査期限算出バッチ 実行スクリプト (GnuCOBOL 3.x)
#   使い方: ./run.sh [FORMATIONS.DAT へのパス] [期待する INSPDUE.DAT のパス | -]
#   既定値: data/FORMATIONS.DAT, expected/INSPDUE.DAT
#   出力  : work/INSPDUE.DAT, work/RC.TXT, work/SYSOUT.TXT, work/SYSERR.TXT
set -e
CALLER_DIR=$(pwd)
SCRIPT_DIR=$(CDPATH= cd "$(dirname "$0")" && pwd)
if [ "$#" -ge 1 ]; then
  INPUT=$1
  case "$INPUT" in
    /*) ;;
    *) INPUT=$CALLER_DIR/$INPUT ;;
  esac
else
  INPUT=$SCRIPT_DIR/data/FORMATIONS.DAT
fi
if [ "$#" -ge 2 ]; then
  EXPECTED=$2
  case "$EXPECTED" in
    -|/*) ;;
    *) EXPECTED=$CALLER_DIR/$EXPECTED ;;
  esac
else
  EXPECTED=$SCRIPT_DIR/expected/INSPDUE.DAT
fi

cd "$SCRIPT_DIR"
mkdir -p work
rm -f work/FORMATIONS.DAT work/INSPDUE.DAT work/SYSOUT.TXT work/SYSERR.TXT work/RC.TXT
cobc -x -o work/urins01 URINS01.cbl
if [ -f "$INPUT" ]; then
  cp "$INPUT" work/FORMATIONS.DAT
fi
cd work
set +e
./urins01 >SYSOUT.TXT 2>SYSERR.TXT
RC=$?
set -e
printf '%s\n' "$RC" > RC.TXT
cat SYSOUT.TXT
cat SYSERR.TXT >&2
echo "RC=$RC"
[ "$RC" -ge 8 ] && exit "$RC"
if [ "$EXPECTED" != "-" ] && [ -f "$EXPECTED" ]; then
  if diff -q INSPDUE.DAT "$EXPECTED" >/dev/null; then
    echo "GOLDEN OK: INSPDUE.DAT matches expected/INSPDUE.DAT"
  else
    echo "GOLDEN MISMATCH:"; diff INSPDUE.DAT "$EXPECTED" || true; exit 1
  fi
fi
exit 0
