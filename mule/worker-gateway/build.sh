#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_dir="$(cd "$project_dir/../.." && pwd)"
if [[ -z "${JAVA_HOME:-}" && -x /usr/libexec/java_home ]]; then
    export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
fi
if [[ -n "${MAVEN_BIN:-}" ]]; then
    maven_bin="$MAVEN_BIN"
elif command -v mvn >/dev/null 2>&1; then
    maven_bin="$(command -v mvn)"
elif [[ -x '/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn' ]]; then
    maven_bin='/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn'
else
    printf 'Maven 3.9+ is required; install Maven or set MAVEN_BIN.\n' >&2
    exit 1
fi

exec "$maven_bin" -B -ntp "-Dmaven.repo.local=$repo_dir/.maven-repository" \
    -f "$project_dir/pom.xml" "$@" package
