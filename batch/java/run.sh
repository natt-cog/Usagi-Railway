#!/bin/sh
# URINS01 Java 実行スクリプト
#   使い方: ./run.sh [FORMATIONS.DAT へのパス] [期待する INSPDUE.DAT のパス | -]
#   既定値: data/FORMATIONS.DAT, expected/INSPDUE.DAT
#   出力  : work/INSPDUE.DAT, work/RC.TXT, work/SYSOUT.TXT, work/SYSERR.TXT
set -e
CALLER_DIR=$(pwd)
SCRIPT_DIR=$(CDPATH= cd "$(dirname "$0")" && pwd)
REPO_ROOT=$(CDPATH= cd "$SCRIPT_DIR/../.." && pwd)
DEFAULT_INPUT=$SCRIPT_DIR/data/FORMATIONS.DAT
DEFAULT_EXPECTED=$SCRIPT_DIR/expected/INSPDUE.DAT

if [ "$#" -ge 1 ]; then
  INPUT=$1
  case "$INPUT" in
    /*) ;;
    *) INPUT=$CALLER_DIR/$INPUT ;;
  esac
else
  INPUT=$DEFAULT_INPUT
fi
if [ "$#" -ge 2 ]; then
  EXPECTED=$2
  case "$EXPECTED" in
    -|/*) ;;
    *) EXPECTED=$CALLER_DIR/$EXPECTED ;;
  esac
else
  EXPECTED=$DEFAULT_EXPECTED
fi

if [ -n "${URMS_WAR+x}" ]; then
  WAR=$URMS_WAR
else
  find_war() {
    find "$REPO_ROOT/target" -maxdepth 1 -type f -name 'usagi-railway*.war' \
      -printf '%T@ %p\n' 2>/dev/null | sort -nr | sed -n '1s/^[^ ]* //p'
  }
  WAR=$(find_war)
  BUILD=0
  if [ -z "$WAR" ]; then
    BUILD=1
  elif [ -n "$(find "$REPO_ROOT/src/main" "$REPO_ROOT/pom.xml" -newer "$WAR" -print -quit)" ]; then
    BUILD=1
  fi
  if [ "$BUILD" -eq 1 ]; then
    (cd "$REPO_ROOT" && mvn -B -q -DskipTests package) >&2
    WAR=$(find_war)
  fi
  if [ -z "$WAR" ]; then
    echo "URINS01 Java: WAR not found under $REPO_ROOT/target" >&2
    exit 1
  fi
  URMS_WAR=$WAR
fi
export URMS_WAR

if [ -z "${URMS_JAVA+x}" ] && [ -n "${JAVA_HOME:-}" ]; then
  URMS_JAVA=$JAVA_HOME/bin/java
fi
export URMS_JAVA

cd "$SCRIPT_DIR"
mkdir -p work
rm -f work/FORMATIONS.DAT work/INSPDUE.DAT work/SYSOUT.TXT work/SYSERR.TXT work/RC.TXT
if [ -f "$INPUT" ]; then
  cp "$INPUT" work/FORMATIONS.DAT
fi
cd work
set +e
"$SCRIPT_DIR/urins01" >SYSOUT.TXT 2>SYSERR.TXT
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
