 seq 1 40 | xargs -n 1 -P 2 sh -c '
    reply=$(curl --http1.1 -ksS --connect-timeout 3 --max-time 10 \
        -H "Connection: close" \
        -w " HTTP=%{http_code}" \
        https://127.0.0.1:29443/health)
    result=$?
    printf "%s exit=%s\n" "$reply" "$result"
' sh

sockstat -4 -l -p 19081 
  read -r -p "PID Java второго WildFly из sockstat: " WF2_PID
ps -p "$WF2_PID" -o pid,comm
kill -TERM "$WF2_PID"


 seq 1 40 | xargs -n 1 -P 2 sh -c '
    reply=$(curl --http1.1 -ksS --connect-timeout 3 --max-time 10 \
        -H "Connection: close" \
        -w " HTTP=%{http_code}" \
        https://127.0.0.1:29443/health)
    result=$?
    printf "%s exit=%s\n" "$reply" "$result"
' sh

export NODE_ID="hr-2"
export NODE_HTTP_PORT="19081"
export NODE_HTTPS_PORT="19445"
export NODE_MANAGEMENT_PORT="19991"
export NODE_RECOVERY_PORT="14812"
export NODE_STATUS_PORT="14813"
export NODE_BASE="$APP_HOME/wildfly-2"

nohup env RUN_CONF=/dev/null COMMON_CONF=/dev/null JAVA_OPTS="$WF2_JAVA_OPTS $WF_MODULE_OPTS $WF_EXTRA" "$WILDFLY_HOME/bin/standalone.sh" -Djboss.server.base.dir="$NODE_BASE" --server-config=standalone.xml -b=127.0.0.1 -bmanagement=127.0.0.1 > "$APP_HOME/logs/wildfly-2.log" 2>&1 < /dev/null &
WF2_LAUNCH_PID=$!
tail -f "$APP_HOME/logs/wildfly-2.log"



 seq 1 40 | xargs -n 1 -P 2 sh -c '
    reply=$(curl --http1.1 -ksS --connect-timeout 3 --max-time 10 \
        -H "Connection: close" \
        -w " HTTP=%{http_code}" \
        https://127.0.0.1:29443/health)
    result=$?
    printf "%s exit=%s\n" "$reply" "$result"
' sh
