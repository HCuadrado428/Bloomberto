#!/usr/bin/env bash
# Starts a dedicated server for each target (or the ones given as arguments) and checks that
# Bloomberto initializes. Usage: scripts/smoke-test.sh [1.21.1-fabric ...]
set -u
cd "$(dirname "$0")/.."
targets=("$@")
[ ${#targets[@]} -eq 0 ] && targets=(1.20.1-fabric 1.20.1-forge 1.21.1-fabric 1.21.1-forge 1.21.1-neoforge)
fail=0
for t in "${targets[@]}"; do
  dir="versions/$t/run"; log="$(mktemp)"
  mkdir -p "$dir"; echo "eula=true" > "$dir/eula.txt"; echo "online-mode=false" > "$dir/server.properties"
  echo "== $t"
  setsid ./gradlew ":$t:runServer" --no-daemon > "$log" 2>&1 &
  pid=$!
  result=FAIL
  for _ in $(seq 1 120); do
    if grep -q "Initializing Bloomberto" "$log" \
       && grep -q "debugEvents.*level_load" "$log" && grep -q "debugEvents.*server_tick" "$log"; then result=OK; break; fi
    if grep -qE "BUILD FAILED|FAILURE:" "$log"; then break; fi
    sleep 5
  done
  kill -- -"$pid" 2>/dev/null; pkill -f "gradle.*runServer" 2>/dev/null; sleep 3
  if [ "$result" = OK ]; then grep -E "Initializing Bloomberto|debugEvents" "$log" | head -4; else fail=1; tail -30 "$log"; fi
  echo "$t: $result"
done
exit $fail
