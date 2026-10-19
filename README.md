# SOA — лабораторная работа №3

![Вариант №1](variants/soa%20lab-3%20var.png)

## 1. Запуск проекта

#### Размещение файлов на сервере

```text
/home/studs/sXXXXXX/
  webLab2/wildfly-33.0.2.Final/    общая установка для двух JVM WildFly
  soa/apache-tomcat-10.1.60/      установка Tomcat
  soa3/
    worker-service.war          исходный артефакт worker HTTP
    worker.ear                  исходный артефакт worker EJB
    hr.ear                      один артефакт для обоих узлов HR
    freebsd/                    consul.hcl, haproxy.cfg, tomcat-server.xml, schema.sql
    tools/consul                Consul 1.21.5 для FreeBSD amd64
    tools/haproxy               HAProxy 3.0.30, собранный на Helios
    tls/                        worker.p12, worker.crt, worker-truststore.p12, hr.p12
    tomcat/                     CATALINA_BASE; приложение в webapps/ROOT.war
    wildfly-1/                  отдельные configuration, data, log, tmp, deployments
    wildfly-2/                  отдельные configuration, data, log, tmp, deployments
    logs/                       вывод фоновых процессов
    run/                        могут оставаться старые PID-файлы; не считать их достоверными
```

#### Окно A — PowerShell на Windows: подключение к Helios

```powershell
ssh -p 2222 sxxxxxx@se.ifmo.ru
```

Далее все блоки `bash` выполняются **на FreeBSD в Bash**, а не в локальном PowerShell.

#### Перед запуском

```bash
ps -U "$(id -un)" -o pid,args
sockstat -4 -l
du -sk "$HOME/soa3"/* 2>/dev/null | sort -n
df -h "$HOME/soa3"
df -i "$HOME/soa3"
```
#### Общие переменные 

```bash
export APP_HOME="$HOME/soa3"
export ARTIFACTS="$APP_HOME"
export JAVA_HOME="/usr/local/openjdk17"
export PATH="$JAVA_HOME/bin:$PATH"
export WILDFLY_HOME="$HOME/webLab2/wildfly-33.0.2.Final"
export CATALINA_HOME="$HOME/soa/apache-tomcat-10.1.60"
export CATALINA_BASE="$APP_HOME/tomcat"
export CONSUL_BIN="$APP_HOME/tools/consul"
export HAPROXY_BIN="$APP_HOME/tools/haproxy"
export CONSUL_HTTP_ADDR="http://127.0.0.1:18500"

export WORKER_DB_URL="jdbc:postgresql://127.0.0.1:5432/studs"
export WORKER_DB_USERNAME="sXXXXXX"
export WORKER_DB_POOL_MAX="3"
export WORKER_EJB_POOL_MAX="4"
export HR_EJB_POOL_MAX="3"
export WORKER_EJB_USERNAME="worker-client"
export WORKER_EJB_URL="remote+http://127.0.0.1:19080"
export WORKER_PUBLIC_URL="https://127.0.0.1:18443"
export WORKER_INSTANCE_ID="worker-1"
export WORKER_SERVICE_CONNECT_TIMEOUT="3s"
export WORKER_SERVICE_READ_TIMEOUT="5s"
export WORKER_CORS_ALLOWED_ORIGINS="https://localhost:19443"
export HR_CORS_ALLOWED_ORIGINS="https://localhost:19443"

read -r -s -p "Пароль PostgreSQL: " WORKER_DB_PASSWORD
printf '\n'
export WORKER_DB_PASSWORD
read -r -s -p "Пароль EJB-пользователя worker-client: " WORKER_EJB_PASSWORD
printf '\n'
export WORKER_EJB_PASSWORD
read -r -s -p "Пароль TLS-хранилищ: " TLS_PASSWORD
printf '\n'
export TLS_PASSWORD

unset _JAVA_OPTIONS
export TOMCAT_JAVA_OPTS="-Xms32m -Xmx128m -Xss512k -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=48m -XX:MaxDirectMemorySize=32m -XX:+UseSerialGC -XX:ActiveProcessorCount=2"
export WF1_JAVA_OPTS="-Xms64m -Xmx256m -Xss512k -XX:MaxMetaspaceSize=192m -XX:ReservedCodeCacheSize=64m -XX:MaxDirectMemorySize=48m -XX:+UseSerialGC -XX:ActiveProcessorCount=2"
export WF2_JAVA_OPTS="-Xms32m -Xmx192m -Xss512k -XX:MaxMetaspaceSize=160m -XX:ReservedCodeCacheSize=48m -XX:MaxDirectMemorySize=32m -XX:+UseSerialGC -XX:ActiveProcessorCount=2"

export WF_MODULE_OPTS="--add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.invoke=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED"
export WF_MODULE_OPTS="$WF_MODULE_OPTS --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED --add-opens=java.base/java.security=ALL-UNNAMED"
export WF_MODULE_OPTS="$WF_MODULE_OPTS --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.util.concurrent=ALL-UNNAMED --add-opens=java.management/javax.management=ALL-UNNAMED --add-opens=java.naming/javax.naming=ALL-UNNAMED"
export WF_MODULE_OPTS="$WF_MODULE_OPTS --add-exports=java.desktop/sun.awt=ALL-UNNAMED --add-exports=java.naming/com.sun.jndi.ldap=ALL-UNNAMED --add-exports=java.naming/com.sun.jndi.url.ldap=ALL-UNNAMED --add-exports=java.naming/com.sun.jndi.url.ldaps=ALL-UNNAMED --add-exports=jdk.naming.dns/com.sun.jndi.dns=ALL-UNNAMED"

export WF_EXTRA="-Djava.awt.headless=true -Djava.net.preferIPv4Stack=true -Djava.util.concurrent.ForkJoinPool.common.parallelism=1"
export WF_EXTRA="$WF_EXTRA -Djavax.net.ssl.trustStore=$APP_HOME/tls/worker-truststore.p12 -Djavax.net.ssl.trustStorePassword=$TLS_PASSWORD -Djavax.net.ssl.trustStoreType=PKCS12"
export CLI_JAVA_OPTS="-Xms16m -Xmx128m -XX:MaxMetaspaceSize=96m -XX:+UseSerialGC -XX:ActiveProcessorCount=1"
export CATALINA_OPTS="$TOMCAT_JAVA_OPTS -Djava.awt.headless=true -Dsoa.tls.password=$TLS_PASSWORD -Dorg.jboss.ejb.client.invocation.timeout=10000 -Dworker.cors.allowed-origins=https://localhost:19443"

umask 077
cd "$APP_HOME"
```

```bash
ls -l "$WILDFLY_HOME/bin/standalone.sh" "$CATALINA_HOME/bin/catalina.sh"
ls -l "$APP_HOME/wildfly-1/configuration/standalone.xml" "$APP_HOME/wildfly-2/configuration/standalone.xml"
ls -l "$APP_HOME/freebsd/consul.hcl" "$APP_HOME/freebsd/haproxy.cfg" "$CATALINA_BASE/conf/server.xml"
ls -l "$APP_HOME/tls/worker.crt" "$APP_HOME/tls/worker-truststore.p12"
ls -l "$CATALINA_BASE/conf/worker.p12" "$CATALINA_BASE/webapps/ROOT.war"
ls -l "$APP_HOME/wildfly-1/configuration/hr.p12" "$APP_HOME/wildfly-2/configuration/hr.p12"
"$CONSUL_BIN" version
"$HAPROXY_BIN" -vv
```

#### Consul

```bash
mkdir -p "$APP_HOME/logs" "$APP_HOME/consul-data"
nohup env GOMAXPROCS=2 GOMEMLIMIT=96MiB SSL_CERT_FILE="$APP_HOME/tls/worker.crt" "$CONSUL_BIN" agent -config-file="$APP_HOME/freebsd/consul.hcl" -data-dir="$APP_HOME/consul-data" > "$APP_HOME/logs/consul.log" 2>&1 < /dev/null &
CONSUL_PID=$!
```

Проверка: 

```bash
tail -n 30 "$APP_HOME/logs/consul.log"
curl -fsS http://127.0.0.1:18500/v1/status/leader
```


#### WildFly 1

```bash
export NODE_ID="hr-1"
export NODE_HTTP_PORT="19080"
export NODE_HTTPS_PORT="19444"
export NODE_MANAGEMENT_PORT="19990"
export NODE_RECOVERY_PORT="14712"
export NODE_STATUS_PORT="14713"
export NODE_BASE="$APP_HOME/wildfly-1"

nohup env STANDALONE_CONF=/dev/null COMMON_CONF=/dev/null JAVA_OPTS="$WF1_JAVA_OPTS $WF_MODULE_OPTS $WF_EXTRA" "$WILDFLY_HOME/bin/standalone.sh" -Djboss.server.base.dir="$NODE_BASE" --server-config=standalone.xml -b=127.0.0.1 -bmanagement=127.0.0.1 > "$APP_HOME/logs/wildfly-1.log" 2>&1 < /dev/null &
WF1_LAUNCH_PID=$!
tail -f "$APP_HOME/logs/wildfly-1.log"
```

Дождитесь завершения старта. `Ctrl+C` при просмотре через `tail -f` закрывает только просмотр; фоновый WildFly продолжает работать. Одного сообщения `started` недостаточно: проверьте deployments.

В Bash откройте CLI работающего сервера:

```bash
JAVA_OPTS="$CLI_JAVA_OPTS" "$WILDFLY_HOME/bin/jboss-cli.sh" --connect --controller=127.0.0.1:19990
```

**Следующий блок — в WildFly CLI**,  `[standalone@127.0.0.1:19990 /]`:

```text
deployment-info
```

Ожидаются `postgresql-42.7.4.jar`, `worker.ear` и `hr.ear`, приложения — со статусом `OK`. Deployments, выполненные через CLI, сохраняются в конфигурации и переживают перезапуск.

Только если соответствующего EAR нет в списке, выполните его команду:

```text
deploy /home/studs/sXXXXXX/soa3/worker.ear
deploy /home/studs/sXXXXXX/soa3/hr.ear
```

```text
deployment-info
quit
```

`quit` закрывает CLI, а не сервер. 

Проверка в Bash:

```bash
curl -i http://127.0.0.1:19080/health
```

Ожидается `200` и `hr-1`. Источник подробных ошибок: `tail -n 150 "$APP_HOME/wildfly-1/log/server.log"`.

#### Tomcat / worker HTTP

```bash
nohup env NODE_ID=worker-1 JAVA_OPTS="" CATALINA_OPTS="$CATALINA_OPTS" "$CATALINA_HOME/bin/catalina.sh" run > "$APP_HOME/logs/tomcat.log" 2>&1 < /dev/null &
TOMCAT_PID=$!
tail -f "$APP_HOME/logs/tomcat.log"
```

Дождитесь старта worker, затем завершите только просмотр через `Ctrl+C`.

Если нужно видеть вывод прямо в терминале, вместо фоновой команды можно выполнить `NODE_ID=worker-1 JAVA_OPTS="" "$CATALINA_HOME/bin/catalina.sh" run`. Тогда этот терминал должен оставаться открытым: `Ctrl+C` остановит Tomcat. Не выполняйте оба варианта одновременно. Foreground не отменяет запись внутренних логов Tomcat и необходимость места на диске.

```bash
curl -i --cacert "$APP_HOME/tls/worker.crt" https://127.0.0.1:18443/health
curl -fsS "http://127.0.0.1:18500/v1/health/service/worker-service?passing=true"
```

Ожидается worker `200`, `node=worker-1`, затем непустой массив Consul. Проверка Consul выполняется каждые 15 секунд; сразу после старта массив может быть пуст. Worker health вызывает Remote EJB и запрос к БД.

####  WildFly 2

```bash
export NODE_ID="hr-2"
export NODE_HTTP_PORT="19081"
export NODE_HTTPS_PORT="19445"
export NODE_MANAGEMENT_PORT="19991"
export NODE_RECOVERY_PORT="14812"
export NODE_STATUS_PORT="14813"
export NODE_BASE="$APP_HOME/wildfly-2"

nohup env STANDALONE_CONF=/dev/null COMMON_CONF=/dev/null JAVA_OPTS="$WF2_JAVA_OPTS $WF_MODULE_OPTS $WF_EXTRA" "$WILDFLY_HOME/bin/standalone.sh" -Djboss.server.base.dir="$NODE_BASE" --server-config=standalone.xml -b=127.0.0.1 -bmanagement=127.0.0.1 > "$APP_HOME/logs/wildfly-2.log" 2>&1 < /dev/null &
WF2_LAUNCH_PID=$!
tail -f "$APP_HOME/logs/wildfly-2.log"
```

После старта завершите просмотр лога через `Ctrl+C`. Откройте CLI **второго** узла:

```bash
JAVA_OPTS="$CLI_JAVA_OPTS" "$WILDFLY_HOME/bin/jboss-cli.sh" --connect --controller=127.0.0.1:19991
```

В CLI:

```text
deployment-info
```

Только если `hr.ear` отсутствует:

```text
deploy /home/studs/sXXXXXX/soa3/hr.ear
```

```text
deployment-info
quit
```

На втором узле достаточно `hr.ear` со статусом `OK`. Worker EAR и WorkerDS нужны только первому узлу.

В Bash:

```bash
curl -i http://127.0.0.1:19081/health
```

Ожидается `200` и `hr-2`.

#### 1.3.5. HAProxy

```bash
"$HAPROXY_BIN" -c -f "$APP_HOME/freebsd/haproxy.cfg"
echo $?
```

Код должен быть `0`. Проверяйте его сразу после команды. Затем запустите в фоне без дополнительного файла лога/PID:

```bash
nohup "$HAPROXY_BIN" -db -f "$APP_HOME/freebsd/haproxy.cfg" > /dev/null 2>&1 < /dev/null &
HAPROXY_PID=$!
ps -p "$HAPROXY_PID" -o pid,args
```

Если процесс сразу завершился, запустите `"$HAPROXY_BIN" -db -f "$APP_HOME/freebsd/haproxy.cfg"` в foreground для просмотра ошибки. При успешном foreground-запуске отсутствие вывода нормально; проверяйте запросы из другого терминала. `Ctrl+C` остановит HAProxy.

```bash
for n in 1 2 3 4; do
    curl -ksS -H 'Connection: close' -w '\n' https://127.0.0.1:19443/health
done
```

Ожидаются оба имени: `hr-1` и `hr-2`. Для демонстрации используются отдельные соединения; запросы одного соединения могут попадать на один узел.

### Подключение с компьютера и открытие клиента

#### Окно B — новое окно PowerShell на Windows: туннель

```powershell
ssh -p 2222 -N -o ExitOnForwardFailure=yes -L 18443:127.0.0.1:18443 -L 19443:127.0.0.1:19443 sxxxxxxx@se.ifmo.ru
```
 **Оставьте окно открытым, не нажимайте Ctrl+C.** 

#### Окно C — ещё одно окно PowerShell: проверка

```powershell
curl.exe -ki https://localhost:18443/health
curl.exe -ki https://localhost:19443/health
```

Ожидаются `200`, `worker-1` и один из `hr-1`/`hr-2`. `-k` 

#### Браузер

1. Откройте <https://localhost:18443/health>. Разрешите исключение для созданного в этой работе сертификата worker. Должен открыться JSON `UP`.
2. Откройте <https://localhost:19443/>. Отдельно разрешите сертификат HR.
3. В клиенте нажмите **«Подключение»**. Укажите worker `https://localhost:18443`, HR `https://localhost:19443`, нажмите **«Сохранить»**. Эти адреса сохраняются в localStorage браузера.
4. Дождитесь загрузки списка работников. Клиент уже включён в `hr.ear`; Node.js на сервере запускать не нужно.

Два сервиса имеют разные сертификаты. Разрешение HR не разрешает worker автоматически. Корень `https://localhost:18443/` не является страницей клиента; там возможен `404`. Клиент находится на `19443`.

#### Проверка доступа клиента к worker

```powershell
curl.exe -ki -H "Origin: https://localhost:19443" "https://localhost:18443/workers?page=1&size=1"
```

Ожидаются `200` и `Access-Control-Allow-Origin: https://localhost:19443`. При `403 Invalid CORS request` перезапустите Tomcat с явным `-Dworker.cors.allowed-origins=https://localhost:19443` из раздела 1.2. Пересборка WAR для этого не нужна.

## 2. Остановка проекта

Останавливайте компоненты в порядке **HAProxy → Tomcat → WildFly 2 → WildFly 1 → Consul**, после чего закройте туннель. Команды выполняйте по шагам, дожидаясь завершения каждого компонента. PostgreSQL и приложения других лабораторных не останавливайте.

### Подключение и переменные

В Bash на FreeBSD выполните следующий блок. Он не зависит от переменных, оставшихся после запуска; вводить пароли БД, EJB и TLS для остановки не нужно:

```bash
export APP_HOME="$HOME/soa3"
export JAVA_HOME="/usr/local/openjdk17"
export PATH="$JAVA_HOME/bin:$PATH"
export WILDFLY_HOME="$HOME/webLab2/wildfly-33.0.2.Final"
export CONSUL_BIN="$APP_HOME/tools/consul"
export CLI_JAVA_OPTS="-Xms16m -Xmx128m -XX:MaxMetaspaceSize=96m -XX:+UseSerialGC -XX:ActiveProcessorCount=1"
unset _JAVA_OPTIONS
ps -U "$(id -un)" -o pid,args
```

PID берите из текущего списка процессов. Значения `$!`, переменные PID и файлы от прошлого запуска могут устареть. Не используйте `killall java`: это затронет другие приложения.

### HAProxy

Найдите процесс с командой `$APP_HOME/tools/haproxy` и конфигурацией `$APP_HOME/freebsd/haproxy.cfg`. Введите его числовой PID и ещё раз проверьте команду:

```bash
read -r -p "PID HAProxy этой лабораторной: " HAPROXY_PID
ps -p "$HAPROXY_PID" -o pid,args
```

```bash
kill -USR1 "$HAPROXY_PID"
```

```bash
ps -p "$HAPROXY_PID" -o pid,args
```

После завершения строки с процессом быть не должно. Если компонент уже остановлен, пропустите его шаг; не вводите произвольный PID.

### Tomcat

Снова посмотрите список процессов:

```bash
ps -U "$(id -un)" -o pid,args
```

Найдите **Java-процесс Tomcat** с `-Dcatalina.base=/home/studs/sXXXXXX/soa3/tomcat`. Введите его PID и проверьте:

```bash
read -r -p "PID Java-процесса Tomcat этой лабораторной: " TOMCAT_PID
ps -p "$TOMCAT_PID" -o pid,args
```

После проверки выполните:

```bash
kill -TERM "$TOMCAT_PID"
```

Дождитесь завершения Tomcat. При штатном закрытии worker снимает регистрацию в Consul. Проверьте:

```bash
ps -p "$TOMCAT_PID" -o pid,args
tail -n 30 "$APP_HOME/logs/tomcat.log"
```

### Два WildFly

В Bash остановите второй узел:

```bash
JAVA_OPTS="$CLI_JAVA_OPTS" "$WILDFLY_HOME/bin/jboss-cli.sh" --connect --controller=127.0.0.1:19991 --command=:shutdown
```

Дождитесь сообщения об остановке в логе:

```bash
tail -n 30 "$APP_HOME/wildfly-2/log/server.log"
```

Затем остановите первый узел:

```bash
JAVA_OPTS="$CLI_JAVA_OPTS" "$WILDFLY_HOME/bin/jboss-cli.sh" --connect --controller=127.0.0.1:19990 --command=:shutdown
tail -n 30 "$APP_HOME/wildfly-1/log/server.log"
```

Ожидаются ответ CLI `"outcome" => "success"` и сообщение `WFLYSRV0050` об остановке в каждом логе. Если CLI не подключается, проверьте процессы и порты: сервер мог быть уже выключен; одна ошибка подключения не подтверждает остановку.

### Consul

Когда приложения остановлены, завершите Consul:

```bash
"$CONSUL_BIN" leave -http-addr=http://127.0.0.1:18500
```

### Проверка и закрытие окон

```bash
ps -U "$(id -un)" -o pid,args
sockstat -4 -l
```