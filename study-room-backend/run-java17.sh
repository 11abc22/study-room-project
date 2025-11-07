#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAVA17_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
JAVA17_BIN="/opt/homebrew/opt/openjdk@17/bin"

if [[ ! -x "$JAVA17_HOME/bin/java" ]]; then
  echo "Java 17 not found at: $JAVA17_HOME" >&2
  echo "Install it with: brew install openjdk@17" >&2
  exit 1
fi

export JAVA_HOME="$JAVA17_HOME"
export PATH="$JAVA17_BIN:$PATH"

cd "$SCRIPT_DIR"

COMMAND="${1:-run}"
shift || true

case "$COMMAND" in
  run)
    exec ./mvnw spring-boot:run "$@"
    ;;
  compile)
    exec ./mvnw -q -DskipTests compile "$@"
    ;;
  test)
    exec ./mvnw test "$@"
    ;;
  version)
    exec ./mvnw -v
    ;;
  *)
    echo "Usage: ./run-java17.sh [run|compile|test|version] [extra mvn args...]" >&2
    exit 1
    ;;
esac
