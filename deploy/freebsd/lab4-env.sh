#!/usr/bin/env bash
if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then
    printf 'Load this file with: source "$HOME/soa4/lab4-env.sh"\n' >&2
    exit 1
fi

export APP_HOME="$HOME/soa3"
export ARTIFACTS="$APP_HOME"
export JAVA_HOME=/usr/local/openjdk17
export PATH="$JAVA_HOME/bin:$PATH"
export WILDFLY_HOME="$HOME/webLab2/wildfly-33.0.2.Final"
export CATALINA_HOME="$HOME/soa/apache-tomcat-10.1.60"
export CATALINA_BASE="$APP_HOME/tomcat"
export CONSUL_BIN="$APP_HOME/tools/consul"
export HAPROXY_BIN="$APP_HOME/tools/haproxy"
export CONSUL_HTTP_ADDR=http://127.0.0.1:18500

export WORKER_DB_URL=jdbc:postgresql://127.0.0.1:5432/studs
export WORKER_DB_USERNAME="$(id -un)"
export WORKER_DB_POOL_MAX=3
export WORKER_EJB_POOL_MAX=4
export HR_EJB_POOL_MAX=3
export WORKER_EJB_USERNAME=worker-client
export WORKER_EJB_URL=remote+http://127.0.0.1:19080
export WORKER_PUBLIC_URL=https://127.0.0.1:28443
export WORKER_INSTANCE_ID=worker-1
export WORKER_SERVICE_CONNECT_TIMEOUT=3s
export WORKER_SERVICE_READ_TIMEOUT=8s
export WORKER_ESB_URL=http://127.0.0.1:18600
export WORKER_CORS_ALLOWED_ORIGINS=https://localhost:19443
export HR_CORS_ALLOWED_ORIGINS=https://localhost:19443

soa_lab4_read_secret() {
    local variable_name="$1" prompt="$2"
    if [[ -z "${!variable_name:-}" ]]; then
        if ! read -r -s -p "$prompt" "$variable_name"; then
            printf '\nUnable to read %s.\n' "$variable_name" >&2
            return 1
        fi
        printf '\n'
    fi
    if [[ -z "${!variable_name:-}" ]]; then
        printf '%s must not be empty.\n' "$variable_name" >&2
        return 1
    fi
    export "$variable_name"
}

soa_lab4_read_secret WORKER_DB_PASSWORD 'Пароль PostgreSQL: ' || return 1
soa_lab4_read_secret WORKER_EJB_PASSWORD 'Пароль EJB worker-client: ' || return 1
soa_lab4_read_secret TLS_PASSWORD 'Пароль TLS-хранилищ worker/hr: ' || return 1
unset -f soa_lab4_read_secret

unset _JAVA_OPTIONS JAVA_TOOL_OPTIONS JDK_JAVA_OPTIONS
soa_lab4_scripts_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$soa_lab4_scripts_dir/java-memory.sh" || return 1
unset soa_lab4_scripts_dir
export WF_MODULE_OPTS='--add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.invoke=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED --add-opens=java.base/java.security=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.util.concurrent=ALL-UNNAMED --add-opens=java.management/javax.management=ALL-UNNAMED --add-opens=java.naming/javax.naming=ALL-UNNAMED --add-exports=java.desktop/sun.awt=ALL-UNNAMED --add-exports=java.naming/com.sun.jndi.ldap=ALL-UNNAMED --add-exports=java.naming/com.sun.jndi.url.ldap=ALL-UNNAMED --add-exports=java.naming/com.sun.jndi.url.ldaps=ALL-UNNAMED --add-exports=jdk.naming.dns/com.sun.jndi.dns=ALL-UNNAMED'
export WF_EXTRA="-Djava.awt.headless=true -Djava.net.preferIPv4Stack=true -Djava.util.concurrent.ForkJoinPool.common.parallelism=1 -Djavax.net.ssl.trustStore=$APP_HOME/tls/worker-truststore.p12 -Djavax.net.ssl.trustStorePassword=$TLS_PASSWORD -Djavax.net.ssl.trustStoreType=PKCS12"
export CATALINA_OPTS="$TOMCAT_JAVA_OPTS -Djava.awt.headless=true -Dsoa.tls.password=$TLS_PASSWORD -Dorg.jboss.ejb.client.invocation.timeout=10000 -Dworker.cors.allowed-origins=https://localhost:19443"

umask 077
printf 'Lab environment loaded; WORKER_ESB_URL=%s\n' "$WORKER_ESB_URL"
