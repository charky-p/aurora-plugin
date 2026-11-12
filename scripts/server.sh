#!/usr/bin/env bash
# Build and run the local test server from Git Bash
#   scripts/server.sh build            mvn package, copies the jar and YAML into LocalServer
#   scripts/server.sh start            starts in the background, returns once it is Done
#   scripts/server.sh stop             clean stop, so onDisable saves everything
#   scripts/server.sh restart          stop, build, start
#   scripts/server.sh cmd "<command>"  runs a console command, e.g. "op Charky"
#   scripts/server.sh status
#   scripts/server.sh backup           database and worlds into LocalServer/aurora-backups
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SERVER="$ROOT/LocalServer"

MVN="/c/Program Files/JetBrains/IntelliJ IDEA 2025.3.1.1/plugins/maven-plugin/lib/maven3/bin/mvn.cmd"
BUILD_JDK='C:\Users\Chill\.jdks\openjdk-25.0.1'
SERVER_JAVA="/c/Users/Chill/.jdks/openjdk-25.0.1/bin/java.exe"
PAPER_JAR="versions/26.3/paper-26.3-248.jar"

# The server reads its console from this file, so commands can be appended to it
CONSOLE="$SERVER/console.in"
OUTPUT="$SERVER/console.out"
PIDFILE="$SERVER/server.pid"

running() {
    [[ -f "$PIDFILE" ]] && kill -0 "$(cat "$PIDFILE")" 2>/dev/null
}

# Catches a server this script did not start, e.g. one from start.bat or IntelliJ
port_in_use() {
    netstat -ano | grep -qE ":25565 .*LISTENING"
}

build() {
    if running || port_in_use; then
        echo "Server is running, stop it first, the build overwrites the jar it has open"
        exit 1
    fi
    (cd "$ROOT" && JAVA_HOME="$BUILD_JDK" "$MVN" -q package)
    echo "Built"
}

start() {
    if running; then echo "Already running"; return; fi
    if port_in_use; then echo "Port 25565 is taken by a server this script did not start"; exit 1; fi

    : > "$CONSOLE"
    : > "$OUTPUT"
    cd "$SERVER"
    # Detached from this script's output, or anything piping it would wait on the server
    tail -n 0 -f "$CONSOLE" 2>/dev/null < /dev/null | "$SERVER_JAVA" -Xmx2G -Xms2G -jar "$PAPER_JAR" nogui > "$OUTPUT" 2>&1 &
    echo $! > "$PIDFILE"

    for _ in $(seq 1 180); do
        if grep -q "Done (" "$OUTPUT"; then
            grep -E "\[Aurora\].*(enabled|SEVERE|Exception)|Done \(" "$OUTPUT" || true
            return
        fi
        if ! running; then
            echo "Server exited during startup:"
            tail -n 20 "$OUTPUT"
            exit 1
        fi
        sleep 1
    done
    echo "No Done line after 180s, check $OUTPUT"
    exit 1
}

stop() {
    if ! running; then
        if port_in_use; then echo "That server was not started by this script, use /stop in-game"; exit 1; fi
        echo "Not running"; return
    fi

    echo "stop" >> "$CONSOLE"
    for _ in $(seq 1 90); do
        if ! running; then
            # Wakes the console tail so it finds the server gone and exits too
            echo >> "$CONSOLE"
            rm -f "$PIDFILE"
            echo "Stopped"
            return
        fi
        sleep 1
    done
    echo "Still running after 90s, check $OUTPUT"
    exit 1
}

# Saving is paused while the worlds are copied, so no file is caught mid-write
backup() {
    if running; then
        echo "save-off" >> "$CONSOLE"
        echo "save-all flush" >> "$CONSOLE"
        for _ in $(seq 1 60); do
            grep -q "Saved the game" "$OUTPUT" && break
            sleep 1
        done
    fi
    python "$ROOT/scripts/deploy.py" backup "$SERVER" || true
    if running; then echo "save-on" >> "$CONSOLE"; fi
}

case "${1:-}" in
    build)   build ;;
    start)   start ;;
    stop)    stop ;;
    restart) stop; build; start ;;
    cmd)
        running || { echo "Not running"; exit 1; }
        echo "${2:?usage: server.sh cmd \"<command>\"}" >> "$CONSOLE"
        ;;
    backup)  backup ;;
    status)  if running; then echo "Running (pid $(cat "$PIDFILE"))"; else echo "Not running"; fi ;;
    *)       sed -n '2,9p' "$0"; exit 1 ;;
esac
