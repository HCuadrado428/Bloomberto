#!/usr/bin/env bash
# Runs the in-game self-test of the testmod (see src/testmod/.../SelfTest.java).
#   scripts/selftest.sh server 1.20.1-fabric [more targets...]   dedicated server checks
#   scripts/selftest.sh client 1.21.1-fabric [more targets...]   singleplayer: client + integrated server checks (needs xvfb-run)
# Prints each target's "[selftest] ... done" lines and exits non-zero if a check failed or the run did not finish.
set -u
cd "$(dirname "$0")/.."
side="$1"; shift
fail=0
for t in "$@"; do
  run="versions/$t/run"; log="$(mktemp)"
  mkdir -p "$run/mods"
  ./gradlew -q ":$t:remapTestmodJar" > /dev/null || { echo "$t: build failed"; fail=1; continue; }
  rm -f "$run"/mods/bloomberto-testmod-*.jar
  # Dev runs only load the testmod from run/mods. Forge 1.20.1 runs on SRG names in development, so it needs the -dev jar.
  case "$t" in
    1.20.1-forge) cp "versions/$t/build/libs/"bloomberto-testmod-*+"$t"-dev.jar "$run/mods/" ;;
    *) cp "versions/$t/build/libs/"bloomberto-testmod-*+"$t".jar "$run/mods/" ;;
  esac
  if [ "$side" = server ]; then
    echo "eula=true" > "$run/eula.txt"; printf "online-mode=false\nlevel-name=selftest\n" > "$run/server.properties"
    rm -rf "$run/selftest"
    timeout 600 ./gradlew ":$t:runServer" -PselfTest=server --args=nogui > "$log" 2>&1
  else
    # the client loads a singleplayer world called "testworld"; reuse the world the server self-test created
    if [ ! -d "$run/saves/testworld" ]; then
      if [ ! -d "$run/selftest" ]; then
        echo "eula=true" > "$run/eula.txt"; printf "online-mode=false\nlevel-name=selftest\n" > "$run/server.properties"
        timeout 600 ./gradlew ":$t:runServer" -PselfTest=server --args=nogui > /dev/null 2>&1
      fi
      mkdir -p "$run/saves" && cp -r "$run/selftest" "$run/saves/testworld"
    fi
    timeout 900 env LIBGL_ALWAYS_SOFTWARE=1 MESA_GL_VERSION_OVERRIDE=4.5 MESA_GLSL_VERSION_OVERRIDE=450 \
      xvfb-run -a -s "-screen 0 1280x720x24" ./gradlew ":$t:runClient" -PselfTest=all \
      --args="--quickPlaySingleplayer testworld --username Dev" > "$log" 2>&1
  fi
  grep -E "\[selftest\] (FAIL|.* done)" "$log" | sed "s/^/$t: /"
  stopped=1
  if [ "$side" = server ] && ! grep -q "\[selftest\] SERVER_STOPPED fired" "$log"; then stopped=0; echo "$t: SERVER_STOPPED did not fire"; fi
  done_ok=1
  grep -q "\[selftest\] server done" "$log" || done_ok=0
  if [ "$side" != server ] && ! grep -q "\[selftest\] client done" "$log"; then done_ok=0; fi
  if [ $stopped = 0 ] || [ $done_ok = 0 ] || grep -q "\[selftest\] FAIL" "$log"; then
    fail=1; echo "$t: FAILED (log: $log)"
    if [ -n "${CI:-}" ]; then grep -E "\[selftest\]|Exception|ERROR" "$log" | tail -60; tail -40 "$log"; fi
  else
    echo "$t: OK"
  fi
done
exit $fail
