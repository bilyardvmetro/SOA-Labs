#!/usr/bin/env bash
set -euo pipefail

# Helios injects heap/Metaspace limits through _JAVA_OPTIONS; keep this
# launcher's explicit memory settings instead of inheriting those overrides.
unset _JAVA_OPTIONS JAVA_TOOL_OPTIONS JDK_JAVA_OPTIONS

scripts_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$scripts_dir/java-memory.sh"

# Foreground launcher for the Mule 4.9.0 Kernel distribution.
# Uses Mule's BasicWrapper instead of the bundled native Tanuki wrapper.
export MULE_HOME="${MULE_HOME:-$HOME/soa4/mule-standalone-4.9.0}"
export MULE_BASE="${MULE_BASE:-$MULE_HOME}"
export JAVA_HOME="${JAVA_HOME:-/usr/local/openjdk17}"
export MULE_WORKER_TRUSTSTORE="${MULE_WORKER_TRUSTSTORE:-$HOME/soa3/tls/worker-truststore.p12}"

if [[ ! -x "$JAVA_HOME/bin/java" ]]; then
    printf 'Java executable not found: %s/bin/java; set JAVA_HOME to JDK 17.\n' "$JAVA_HOME" >&2
    exit 1
fi
if [[ ! -d "$MULE_HOME/lib/boot" || ! -d "$MULE_BASE/conf" ]]; then
    printf 'Mule installation not found; check MULE_HOME and MULE_BASE.\n' >&2
    exit 1
fi

cd "$MULE_BASE"
# Truffle 24.1.1 cannot choose its default resource cache on FreeBSD.
# An explicit cache path avoids that OS-dependent lookup without spoofing os.name.
mkdir -p "$MULE_BASE/.polyglot-cache"
read -r -a mule_java_opts <<< "$MULE_JAVA_OPTS"
if [[ -n "${MULE_GATEWAY_PORT:-}" ]]; then
    mule_java_opts+=("-Dgateway.port=$MULE_GATEWAY_PORT")
fi
if [[ -n "${MULE_CONSUL_PORT:-}" ]]; then
    mule_java_opts+=("-Dconsul.port=$MULE_CONSUL_PORT")
fi
exec "$JAVA_HOME/bin/java" \
    "${mule_java_opts[@]}" \
    "-Dmule.home=$MULE_HOME" \
    "-Dmule.base=$MULE_BASE" \
    "-Dpolyglot.engine.userResourceCache=$MULE_BASE/.polyglot-cache" \
    -Dmule.bootstrap.container.wrapper.class=org.mule.runtime.module.boot.internal.MuleContainerBasicWrapper \
    --module-path "$MULE_HOME/lib/boot" \
    --add-modules=java.se,org.mule.runtime.jpms.utils,com.fasterxml.jackson.core \
    --add-opens=java.base/java.lang=org.mule.runtime.jpms.utils \
    --add-opens=java.base/java.lang.reflect=org.mule.runtime.jpms.utils \
    --add-opens=java.base/java.lang.invoke=org.mule.runtime.jpms.utils \
    --add-opens=java.sql/java.sql=org.mule.runtime.jpms.utils \
    --module=org.mule.boot/org.mule.runtime.module.reboot.MuleContainerBootstrap
