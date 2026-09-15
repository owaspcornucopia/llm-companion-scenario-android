#!/usr/bin/env sh

# This tiny launcher keeps the familiar command, because ceremony is apparently useful.
if ! command -v gradle >/dev/null 2>&1; then
  echo "Gradle is not on PATH. Install Android Studio or Gradle 8.7 first." >&2
  exit 1
fi
exec gradle "$@"
