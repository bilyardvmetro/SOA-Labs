# Запуск и остановка SOA 4 на нашем Helios

Стенд `s413041@se.ifmo.ru`: приложения и конфигурация уже установлены, процессы остановлены. PostgreSQL — общий сервер, отдельно его не запускаем.

Выполняй шаги последовательно. После готовности сервера нажимай **Ctrl+C в `tail -f`**, затем выполняй проверочный `curl`. Это закрывает только просмотр лога. Следующий компонент запускай после успешной проверки.

## 1. Mac — скрипты и SSH

```bash
cd /Users/kirill/coding/SOA-Labs

scp -P 2222 \
  deploy/freebsd/lab4-env.sh \
  deploy/freebsd/java-memory.sh \
  deploy/freebsd/run-wildfly.sh \
  deploy/freebsd/run-mule.sh \
  deploy/freebsd/tomcat-setenv.sh \
  s413041@se.ifmo.ru:soa4/

ssh -p 2222 s413041@se.ifmo.ru
```

Далее — **Bash на Helios**, все запуски в одном SSH-окне. Повторная загрузка EAR, WAR и runtime для обычного запуска не нужна.

## 2. Helios — окружение, пароли, диск

```bash
unset _JAVA_OPTIONS JAVA_TOOL_OPTIONS JDK_JAVA_OPTIONS
unset WORKER_DB_PASSWORD WORKER_EJB_PASSWORD TLS_PASSWORD
source "$HOME/soa4/lab4-env.sh"

export MULE_HOME="$HOME/soa4/mule-standalone-4.9.0"
export MULE_BASE="$MULE_HOME"
export MULE_WORKER_TRUSTSTORE="$APP_HOME/tls/worker-truststore.p12"
unset MULE_GATEWAY_PORT MULE_CONSUL_PORT

read -r -s -p "Отдельный пароль worker-truststore.p12 для Mule: " MULE_WORKER_TRUSTSTORE_PASSWORD
printf '\n'
export MULE_WORKER_TRUSTSTORE_PASSWORD

umask 077
mkdir -p "$APP_HOME/logs" "$APP_HOME/consul-data" "$CATALINA_BASE/bin"
cp "$HOME/soa4/tomcat-setenv.sh" "$CATALINA_BASE/bin/setenv.sh"

touch "$APP_HOME/logs/consul.log" "$APP_HOME/logs/wildfly-1.log" \
  "$APP_HOME/logs/tomcat.log" "$APP_HOME/logs/wildfly-2.log" \
  "$APP_HOME/logs/haproxy.log" "$HOME/soa4/mule-startup.log"

/sbin/zfs get userused@s413041,userquota@s413041 export/home/studs
ps -U "$(id -un)" -o pid,ppid,nlwp,rss,ucomm
sockstat -4 -l | grep -E ':(18500|18600|19080|19081|28443|29443)[[:space:]]'
```

`lab4-env.sh` запросит **PostgreSQL**, **EJB worker-client**, **TLS worker/hr**. Пароль **truststore Mule** вводится отдельно; не копируй в него `TLS_PASSWORD`. Пароли вводятся скрыто, в файлы инструкции не записываются. Последний `sockstat` перед запуском должен быть пустым.

Проверка пароля truststore — должна завершиться успешно:

```bash
"$JAVA_HOME/bin/keytool" \
  -J-Xms16m -J-Xmx64m -J-XX:+UseSerialGC -J-XX:ActiveProcessorCount=1 \
  -list -storetype PKCS12 \
  -keystore "$MULE_WORKER_TRUSTSTORE" \
  -storepass:env MULE_WORKER_TRUSTSTORE_PASSWORD
```

Профили из `java-memory.sh`, размеры в МиБ:

| Процесс | Heap начальный / максимум | Metaspace максимум | CPU |
| --- | ---: | ---: | ---: |
| WildFly 1 | 64 / 256 | 192 | 1 |
| Tomcat | 32 / 128 | 128 | 1 |
| Mule | 32 / 256 | 160 | 2 |
| WildFly 2 | 32 / 192 | 160 | 1 |
| WildFly CLI | 16 / 96 | 96 | 1 |

Все JVM используют Serial GC. Heap — только часть RSS. Квота ZFS `1G` — диск. `tomcat-setenv.sh` убирает переопределение лимитов из общего Tomcat; `run-wildfly.sh` задаёт `RUN_CONF=/dev/null`; Mule запускается Java-загрузчиком без native Tanuki.

## 3. Consul

```bash
nohup env GOMAXPROCS=2 GOMEMLIMIT=96MiB \
  SSL_CERT_FILE="$APP_HOME/tls/worker.crt" \
  "$CONSUL_BIN" agent \
  -config-file="$APP_HOME/freebsd/consul.hcl" \
  -data-dir="$APP_HOME/consul-data" \
  > "$APP_HOME/logs/consul.log" 2>&1 < /dev/null &
CONSUL_PID=$!
tail -f "$APP_HOME/logs/consul.log"
```

Дождись `cluster leadership acquired`. **Ctrl+C**, затем:

```bash
curl -i --connect-timeout 2 --max-time 10 \
  http://127.0.0.1:18500/v1/status/leader
```

Ожидается `200`, `"127.0.0.1:18300"`.

## 4. WildFly 1 — worker EJB и HR

```bash
nohup bash "$HOME/soa4/run-wildfly.sh" 1 \
  > "$APP_HOME/logs/wildfly-1.log" 2>&1 < /dev/null &
tail -f "$APP_HOME/logs/wildfly-1.log"
```

Дождись завершения старта без ошибок деплоя. **Ctrl+C**, затем:

```bash
curl -i --connect-timeout 2 --max-time 10 http://127.0.0.1:19080/health

JAVA_OPTS="$CLI_JAVA_OPTS" "$WILDFLY_HOME/bin/jboss-cli.sh" \
  --connect --controller=127.0.0.1:19990 --command=deployment-info
```

Ожидается `200`, `hr-1`; `hr.ear`, `worker.ear`, `postgresql-42.7.4.jar` — `OK`.

## 5. Tomcat — worker HTTPS

```bash
nohup env NODE_ID=worker-1 JAVA_OPTS="" CATALINA_OPTS="$CATALINA_OPTS" \
  "$CATALINA_HOME/bin/catalina.sh" run \
  > "$APP_HOME/logs/tomcat.log" 2>&1 < /dev/null &
tail -f "$APP_HOME/logs/tomcat.log"
```

Дождись `Server startup`. **Ctrl+C**, затем:

```bash
curl -i --connect-timeout 2 --max-time 10 \
  --cacert "$APP_HOME/tls/worker.crt" \
  https://127.0.0.1:28443/health

curl -i --connect-timeout 2 --max-time 10 \
  'http://127.0.0.1:18500/v1/health/service/worker-service?passing=true'
```

Ожидается `200`, `worker-1`; Consul возвращает непустой массив с портом `28443` и `passing`. Проверка Consul выполняется раз в 15 секунд — при пустом массиве повтори второй `curl` позже.

## 6. Mule — worker-gateway

```bash
nohup bash "$HOME/soa4/run-mule.sh" \
  > "$HOME/soa4/mule-startup.log" 2>&1 < /dev/null &
tail -f "$HOME/soa4/mule-startup.log"
```

Дождись **`worker-gateway ... DEPLOYED`** в таблице приложений. **Ctrl+C**, затем:

```bash
curl -i --connect-timeout 2 --max-time 10 http://127.0.0.1:18600/health

curl -i --connect-timeout 2 --max-time 15 \
  http://127.0.0.1:18600/workers/2147483647
```

Ожидается `200`, `worker-gateway`; второй запрос — `422` об отсутствующем работнике. `FAILED` приложения и один лишь `default DEPLOYED` готовность не подтверждают.

## 7. WildFly 2 — HR

```bash
nohup bash "$HOME/soa4/run-wildfly.sh" 2 \
  > "$APP_HOME/logs/wildfly-2.log" 2>&1 < /dev/null &
tail -f "$APP_HOME/logs/wildfly-2.log"
```

Дождись завершения старта без ошибок деплоя. **Ctrl+C**, затем:

```bash
curl -i --connect-timeout 2 --max-time 10 http://127.0.0.1:19081/health

JAVA_OPTS="$CLI_JAVA_OPTS" "$WILDFLY_HOME/bin/jboss-cli.sh" \
  --connect --controller=127.0.0.1:19991 --command=deployment-info
```

Ожидается `200`, `hr-2`; `hr.ear` — `OK`. Worker EJB и PostgreSQL-драйвер нужны первому узлу.

## 8. HAProxy

```bash
"$HAPROXY_BIN" -c -f "$APP_HOME/freebsd/haproxy.cfg"
printf 'Код проверки HAProxy: %s\n' "$?"
```

Продолжай при коде **0**, даже если сама проверка ничего не напечатала.

```bash
nohup "$HAPROXY_BIN" -db -f "$APP_HOME/freebsd/haproxy.cfg" \
  > "$APP_HOME/logs/haproxy.log" 2>&1 < /dev/null &
tail -f "$APP_HOME/logs/haproxy.log"
```

Лог может быть пустым. Через несколько секунд **Ctrl+C**, затем:

```bash
for n in 1 2 3 4; do
  curl -ki --connect-timeout 2 --max-time 10 \
    -H 'Connection: close' -w '\n' \
    https://127.0.0.1:29443/health
done
```

Ожидается `200` и оба имени `hr-1`, `hr-2`.

## 9. Mac — туннель и браузер

В **новом локальном терминале Mac**:

```bash
ssh -p 2222 -N -o ExitOnForwardFailure=yes \
  -L 18443:127.0.0.1:28443 \
  -L 19443:127.0.0.1:29443 \
  s413041@se.ifmo.ru
```

Оставь окно открытым. В ещё одном локальном терминале:

```bash
curl -ki --connect-timeout 2 --max-time 10 https://localhost:18443/health
curl -ki --connect-timeout 2 --max-time 10 https://localhost:19443/health
```

Браузер: сначала [worker health](https://localhost:18443/health), затем [клиент](https://localhost:19443/). Разреши оба сертификата. В «Подключении»: worker `https://localhost:18443`, HR `https://localhost:19443`.

Тестовая запись: зарплата `1000`, статус `PROBATION`, дата начала в будущем, организация `LAB4_TEST`. Индексация `1.10` → `1100`; увольнение из `LAB4_TEST` → `FIRED`; обновление страницы сохраняет результат.

## 10. Проверка цепочки и ресурсов — Helios

```bash
curl -i --connect-timeout 2 --max-time 25 -X POST \
  http://127.0.0.1:19080/hr/index/2147483647/1.10
curl -i --connect-timeout 2 --max-time 25 -X POST \
  http://127.0.0.1:19081/hr/index/2147483647/1.10

ps -U "$(id -un)" -o pid,ppid,nlwp,rss,ucomm
/sbin/zfs get userused@s413041,userquota@s413041 export/home/studs
```

Оба POST — `422`, если работника с таким ID нет. `rss` — КиБ, `nlwp` — потоки. Проверка фактических флагов всех четырёх JVM:

```bash
for pid in $(sockstat -4 -l | awk -v owner="$(id -un)" \
  '$1 == owner && $2 == "java" && $6 ~ /:(19080|19081|28443|18600)$/ {print $3}' | sort -u); do
  printf '\nPID %s\n' "$pid"
  "$JAVA_HOME/bin/jcmd" \
    -J-Xmx64m -J-XX:+UseSerialGC -J-XX:ActiveProcessorCount=1 \
    "$pid" VM.flags | tr ' ' '\n' | \
    grep -E 'UseSerialGC|ActiveProcessorCount|MaxHeapSize|MaxMetaspaceSize'
done
```

У всех `+UseSerialGC`; CPU и лимиты — из таблицы шага 2. Полные аргументы JVM могут содержать пароли — для диагностики используй `VM.flags`.

## Если Consul не стартует после долгого простоя

Только при `server_rejoin_age_max` в логе, для нашего единственного узла. Заверши предыдущий запуск:

```bash
if kill -0 "$CONSUL_PID" 2>/dev/null; then
  kill -TERM "$CONSUL_PID"
  wait "$CONSUL_PID"
fi

mv "$APP_HOME/consul-data/server_metadata.json" \
  "$APP_HOME/consul-data/server_metadata.json.bak-$(date +%Y%m%d-%H%M%S)"
```

Повтори шаг 3. Остальные данные Consul сохраняются. Причина и файл описаны в [документации Consul](https://developer.hashicorp.com/consul/docs/reference/agent/configuration-file/general#server_rejoin_age_max).

## Пояснения к флагам

| Команда / параметр | Значение |
| --- | --- |
| `scp -P 2222`, `ssh -p 2222` | SSH-порт Helios; у `scp` буква заглавная. |
| `ssh -N`, `-L local:host:remote` | Только туннель; локальный порт направляется на указанный адрес с Helios. |
| `-o ExitOnForwardFailure=yes` | Завершить SSH, если не удалось открыть локальные порты туннеля. |
| `read -r -s -p` | Читать буквально, скрыть ввод, показать приглашение. |
| `mkdir -p` | Создать каталог вместе с родительскими; существующий сохранить. |
| `nohup`, `&` | Пережить закрытие SSH; запустить в фоне. |
| `> log 2>&1 < /dev/null` | Перезаписать лог, направить туда ошибки, отключить ввод. |
| `tail -f` | Последние строки лога и дальнейшие изменения; Ctrl+C закрывает просмотр. |
| `env KEY=value` | Задать окружение только запускаемой команде. |
| `GOMAXPROCS=2`, `GOMEMLIMIT=96MiB` | Consul: два одновременно исполняющих Go CPU и мягкая цель памяти Go, не предел RSS. |
| Consul `agent`, `-config-file`, `-data-dir` | Запустить агент; файл конфигурации; каталог постоянного состояния. |
| `SSL_CERT_FILE` | Сертификат доверия для HTTPS-проверки worker в Consul. |
| `curl -i`, `-k` | Показать HTTP-заголовки; пропустить проверку сертификата только для диагностических запросов к самоподписанному HR/локальным URL. |
| `--connect-timeout`, `--max-time` | Максимум секунд на соединение и весь запрос. |
| `--cacert` | Проверять TLS по указанному сертификату; используется для worker. |
| `-H`, `-w`, `-X POST` | HTTP-заголовок; формат вывода после ответа; метод POST. |
| HAProxy `-c`, `-f`, `-db` | Проверить конфигурацию; выбрать файл; работать без собственного ухода в фон. [HAProxy 3.0](https://docs.haproxy.org/3.0/management.html#3) |
| CLI `--connect`, `--controller`, `--command` | Подключиться к WildFly; адрес управления; выполнить одну команду и выйти. |
| `-Xms`, `-Xmx`, `-Xss` | Начальный heap, максимальный heap, stack одного Java-потока. |
| `-XX:+UseSerialGC`, `-XX:ActiveProcessorCount` | Однопоточная сборка мусора; число CPU для расчёта пулов JVM и приложения. |
| `MaxMetaspaceSize`, `CompressedClassSpaceSize` | Предел Metaspace и области сжатых указателей классов. |
| `ReservedCodeCacheSize`, `MaxDirectMemorySize` | Предел code cache и прямых NIO-буферов. |
| `-Dkey=value` | Системное свойство Java; в профиле также ограничен общий ForkJoinPool. |
| `--add-opens`, `--add-exports` | Разрешения доступа к модулям JDK для WildFly. |
| `--module-path`, `--add-modules`, `--module` | Путь модулей, дополнительные модули и точка запуска Mule. |
| `keytool -list -storetype PKCS12 -keystore` | Прочитать записи указанного PKCS12-хранилища. |
| `-storepass:env NAME`, `-J...` | Получить пароль из переменной; передать параметр JVM инструмента. [keytool Java 17](https://docs.oracle.com/en/java/javase/17/docs/specs/man/keytool.html#common-command-options) |
| `ps -U ... -o ...`, `sockstat -4 -l` | Процессы пользователя с выбранными полями; IPv4-слушатели. |
| `grep -E`, `sort -u` | Расширенное регулярное выражение; удалить повторяющиеся PID. |
| `awk -v`, `tr ' ' '\n'` | Передать переменную в фильтр; разнести параметры Java по строкам. |
| `kill -USR1`, `kill -TERM`, `kill -0` | HAProxy завершает соединения; JVM/Consul получают сигнал завершения; проверить существование и доступность процесса. |

## 11. Остановка стенда — Helios

Работает и из нового SSH-сеанса, без паролей. На Mac оставь туннель до окончания остановки.

```bash
stop_port() {
  local name="$1" port="$2" expected_command="$3" signal="$4"
  local pids pid attempt

  pids="$(sockstat -4 -l | awk \
    -v owner="$(id -un)" -v port="$port" -v cmd="$expected_command" \
    '$1 == owner && $2 == cmd && $6 ~ (":" port "$") {print $3}' | sort -u)"

  if [[ -z "$pids" ]]; then
    printf '%s: слушатель отсутствует\n' "$name"
    return 0
  fi

  for pid in $pids; do
    printf 'Останавливаю %s, PID %s\n' "$name" "$pid"
    kill "-$signal" "$pid" || return 1
    for ((attempt=0; attempt<60; attempt++)); do
      kill -0 "$pid" 2>/dev/null || break
      sleep 1
    done
    if kill -0 "$pid" 2>/dev/null; then
      printf '%s ещё работает: проверь лог, затем повтори остановку\n' "$name"
      return 1
    fi
  done
}

stop_port HAProxy    29443 haproxy USR1 &&
stop_port Mule       18600 java    TERM &&
stop_port Tomcat     28443 java    TERM &&
stop_port WildFly-2  19081 java    TERM &&
stop_port WildFly-1  19080 java    TERM &&
stop_port Consul     18500 consul  TERM
```

Проверка:

```bash
sockstat -4 -l | awk -v owner="$(id -un)" \
  '$1 == owner && $6 ~ /:(18300|18301|18302|18500|18600|19080|19081|19444|19445|19990|19991|28443|29443)$/ {print}'

ps -U "$(id -un)" -o pid,ppid,nlwp,rss,ucomm
```

Первый вывод пустой; процессов наших Java, Consul и HAProxy нет. Если процесс не открыл порт из-за ошибки запуска, он виден только в `ps`: заверши именно его текущий PID командой `kill -TERM PID` и дождись исчезновения. Не используй старые PID или `killall java`.

На **Mac** нажми **Ctrl+C в окне SSH-туннеля**. На Helios можно очистить секреты и закрыть соединение:

```bash
unset WORKER_DB_PASSWORD WORKER_EJB_PASSWORD TLS_PASSWORD
unset MULE_WORKER_TRUSTSTORE_PASSWORD WF_EXTRA CATALINA_OPTS JAVA_OPTS
exit
```
