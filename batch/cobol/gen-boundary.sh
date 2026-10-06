#!/bin/sh
set -e

SCRIPT_DIR=$(CDPATH= cd "$(dirname "$0")" && pwd)
REPO_ROOT=$(CDPATH= cd "$SCRIPT_DIR/../.." && pwd)
BOUNDARY_DIR=$REPO_ROOT/src/test/resources/golden/boundary
MANIFEST=$BOUNDARY_DIR/cases.tsv

while IFS="$(printf '\t')" read -r case_name mode java_rc gap description; do
  case "$case_name" in
    ""|\#*) continue ;;
  esac

  case_dir=$BOUNDARY_DIR/$case_name
  rm -f "$case_dir/INSPDUE.DAT" "$case_dir/RC" "$case_dir/STDOUT.txt" \
    "$case_dir/STDERR.txt" "$case_dir/COBOL_RC" "$case_dir/COBOL_STDOUT.txt"

  if "$SCRIPT_DIR/run.sh" "$case_dir/FORMATIONS.DAT" -; then
    :
  else
    run_status=$?
    if [ "$run_status" -lt 8 ]; then
      echo "run.sh failed for $case_name (status $run_status)" >&2
      exit "$run_status"
    fi
  fi

  RC=$(cat "$SCRIPT_DIR/work/RC.TXT")
  case "$mode" in
    golden)
      cp "$SCRIPT_DIR/work/RC.TXT" "$case_dir/RC"
      cp "$SCRIPT_DIR/work/SYSOUT.TXT" "$case_dir/STDOUT.txt"
      if [ -f "$SCRIPT_DIR/work/INSPDUE.DAT" ]; then
        cp "$SCRIPT_DIR/work/INSPDUE.DAT" "$case_dir/INSPDUE.DAT"
      fi
      if [ -s "$SCRIPT_DIR/work/SYSERR.TXT" ]; then
        cp "$SCRIPT_DIR/work/SYSERR.TXT" "$case_dir/STDERR.txt"
      fi
      ;;
    error)
      cp "$SCRIPT_DIR/work/RC.TXT" "$case_dir/COBOL_RC"
      cp "$SCRIPT_DIR/work/SYSOUT.TXT" "$case_dir/COBOL_STDOUT.txt"
      ;;
    *)
      echo "Unknown mode '$mode' for $case_name" >&2
      exit 1
      ;;
  esac
  printf '%s %s RC=%s\n' "$case_name" "$mode" "$RC"
done < "$MANIFEST"

if DEFAULT_OUTPUT=$("$SCRIPT_DIR/run.sh" 2>&1); then
  printf '%s\n' "$DEFAULT_OUTPUT"
else
  run_status=$?
  printf '%s\n' "$DEFAULT_OUTPUT"
  echo "Default run.sh failed (status $run_status)" >&2
  exit "$run_status"
fi
case "$DEFAULT_OUTPUT" in
  *"GOLDEN OK"*) ;;
  *)
    echo "Default run.sh did not print GOLDEN OK" >&2
    exit 1
    ;;
esac
