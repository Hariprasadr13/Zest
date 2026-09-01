#!/bin/sh
# Gradle bootstrap for this repository. Install Gradle 8.10.2+ or use your IDE's Gradle integration.
if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi
echo "Gradle is not installed. Install Gradle 8.10.2+ (or run this project from an IDE with Gradle support)." >&2
exit 1
