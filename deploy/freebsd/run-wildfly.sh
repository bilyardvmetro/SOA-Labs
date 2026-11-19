#!/usr/bin/env bash
set -euo pipefail

node="${1:-}"
if [[ "$node" != 1 && "$node" != 2 ]]; then
    printf 'Usage: bash run-wildfly.sh 1|2\n' >&2
    exit 1
fi
for variable_name in APP_HOME JAVA_HOME WILDFLY_HOME WF_MODULE_OPTS WF_EXTRA WORKER_ESB_URL; do
    if [[ -z "${!variable_name:-}" ]]; then
        printf 'Missing %s. First source "$HOME/soa4/lab4-env.sh".\n' "$variable_name" >&2
        exit 1
    fi
done

scripts_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$scripts_dir/java-memory.sh"
unset _JAVA_OPTIONS JAVA_TOOL_OPTIONS JDK_JAVA_OPTIONS
# WildFly 33 reads RUN_CONF; STANDALONE_CONF does not select its JVM config.
export RUN_CONF=/dev/null
export COMMON_CONF=/dev/null
export JAVA="$JAVA_HOME/bin/java"

export NODE_ID="hr-$node"
export NODE_BASE="$APP_HOME/wildfly-$node"
if [[ "$node" == 1 ]]; then
    export NODE_HTTP_PORT=19080 NODE_HTTPS_PORT=19444 NODE_MANAGEMENT_PORT=19990
    export NODE_RECOVERY_PORT=14712 NODE_STATUS_PORT=14713
    export JAVA_OPTS="$WF1_JAVA_OPTS $WF_MODULE_OPTS $WF_EXTRA"
else
    export NODE_HTTP_PORT=19081 NODE_HTTPS_PORT=19445 NODE_MANAGEMENT_PORT=19991
    export NODE_RECOVERY_PORT=14812 NODE_STATUS_PORT=14813
    export JAVA_OPTS="$WF2_JAVA_OPTS $WF_MODULE_OPTS $WF_EXTRA"
fi

if [[ ! -f "$NODE_BASE/configuration/standalone.xml" ]]; then
    printf 'Configuration not found: %s/configuration/standalone.xml\n' "$NODE_BASE" >&2
    exit 1
fi
exec "$WILDFLY_HOME/bin/standalone.sh" \
    "-Djboss.server.base.dir=$NODE_BASE" --server-config=standalone.xml \
    -b=127.0.0.1 -bmanagement=127.0.0.1
