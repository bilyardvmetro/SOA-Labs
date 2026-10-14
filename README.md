# SOA Labs

Монорепозиторий содержит три части приложения:

- `worker-service` — Spring MVC REST, разворачивается как `ROOT.war` в Apache Tomcat 10.1;
- `hr-service` — Spring MVC REST, разворачивается как `ROOT.war` в WildFly 33;
- `frontend` — Vue 3 + TypeScript + Vite, production-сборка автоматически включается в `hr-service.war`.

Ниже описан чистый запуск на Helios с Java 17 и PostgreSQL 18.

## Используемые порты

| Компонент | Протокол | Порт |
|---|---:|---:|
| worker-service / Tomcat | HTTPS | `18443` |
| hr-service и frontend / WildFly | HTTPS | `19443` |
| WildFly management | HTTP, только `127.0.0.1` | `55054` |

## 1. Что подготовить локально

Нужны:

- каталоги `apache-tomcat-10.1.60` и `wildfly-33.0.2.Final`;

Собрать оба WAR из корня репозитория:

```bash
cd /Users/kirill/coding/SOA-Labs
./gradlew clean :worker-service:bootWar :hr-service:bootWar
```

Получатся файлы:

```text
worker-service/build/libs/worker-service.war
hr-service/build/libs/hr-service.war
```

Задача `:hr-service:bootWar` сама выполняет `npm ci` и `npm run build`, после чего помещает frontend внутрь WAR.

## 2. Передача файлов на Helios

Создать каталоги:

```bash
mkdir -p "/soa"
```

С локального компьютера передать серверы, если их ещё нет на Helios:

```bash
scp -r /путь/к/apache-tomcat-10.1.60 \
  s413041@helios:~/soa

scp -r /путь/к/wildfly-33.0.2.Final \
  s413041@helios:~/soa
```

Передать приложения:

```bash
cd /Users/kirill/coding/SOA-Labs

scp worker-service/build/libs/worker-service.war \
  s413041@helios:~/soa

scp hr-service/build/libs/hr-service.war \
  s413041@helios:~/soa
```

После копирования проверить права запуска:

```bash
ssh s413041@helios
sh

APP_HOME="$HOME/webLab2"
TOMCAT_HOME="$APP_HOME/apache-tomcat-10.1.60"
WILDFLY_HOME="$APP_HOME/wildfly-33.0.2.Final"

chmod +x "$TOMCAT_HOME"/bin/*.sh
chmod +x "$WILDFLY_HOME"/bin/*.sh
```

## 3. Проверка PostgreSQL

Вместо `<ПАРОЛЬ_БД>` необходимо подставить .pgpass пользователя.

Текущая конфигурация приложения использует `spring.jpa.hibernate.ddl-auto=create`, поэтому при перезапуске worker-service таблицы пересоздаются и ранее сохранённые данные могут быть потеряны.

## 4. Сертификат worker-service

Создать PKCS12 keystore для Tomcat. SAN содержит `helios.cs.ifmo.ru` и `127.0.0.1`, потому что доступ из браузера будет выполняться через SSH-туннель:

```bash
keytool -genkeypair \
  -alias worker-service \
  -keyalg RSA \
  -keysize 2048 \
  -validity 3650 \
  -storetype PKCS12 \
  -keystore "$TOMCAT_HOME/conf/worker-service.p12" \
  -storepass changeit \
  -keypass changeit \
  -dname "CN=helios.cs.ifmo.ru, OU=SOA, O=ITMO, C=RU" \
  -ext "SAN=dns:helios.cs.ifmo.ru,ip:127.0.0.1"
```

Экспортировать публичный сертификат worker-service. Он понадобится WildFly для проверки HTTPS-соединения с Tomcat:

```bash
keytool -exportcert \
  -rfc \
  -alias worker-service \
  -keystore "$TOMCAT_HOME/conf/worker-service.p12" \
  -storepass changeit \
  -file "$APP_HOME/worker-service.crt"
```

## 5. Настройка Tomcat

### 5.1. HTTPS Connector

Открыть файл:

```bash
vi "$TOMCAT_HOME/conf/server.xml"
```

Изменить shutdown-порт в корневом элементе `Server`, чтобы не использовать стандартный `8005`:

```xml
<Server port="18005" shutdown="SHUTDOWN">
```

Удалить или закомментировать обычный HTTP Connector на порту `8080`:

```xml
<!--
<Connector port="8080" protocol="HTTP/1.1" ... />
-->
```

Добавить HTTPS Connector внутрь элемента `Service`:

```xml
<Connector
        port="18443"
        protocol="org.apache.coyote.http11.Http11NioProtocol"
        address="0.0.0.0"
        maxThreads="20"
        minSpareThreads="2"
        maxConnections="100"
        acceptCount="10"
        SSLEnabled="true"
        scheme="https"
        secure="true"
        maxParameterCount="1000">
    <SSLHostConfig>
        <Certificate
                certificateKeystoreFile="conf/worker-service.p12"
                certificateKeystorePassword="changeit"
                certificateKeystoreType="PKCS12"
                certificateKeyAlias="worker-service"
                type="RSA" />
    </SSLHostConfig>
</Connector>
```

### 5.2. Память и переменные окружения Tomcat

Файл `setenv.sh` отсутствует в чистом Tomcat — его нужно создать самостоятельно:

```bash
vi "$TOMCAT_HOME/bin/setenv.sh"
```

Содержимое:

```sh
#!/bin/sh

export CATALINA_OPTS="-Xms64m -Xmx256m -Xss512k -XX:MaxMetaspaceSize=128m -XX:ActiveProcessorCount=2 -Djava.awt.headless=true"

export WORKER_DB_URL="jdbc:postgresql://localhost:5432/studs"
export WORKER_DB_USERNAME="s413041"
export WORKER_DB_PASSWORD="<ПАРОЛЬ_БД>"

export WORKER_CORS_ALLOWED_ORIGINS="https://localhost:19443,https://127.0.0.1:19443,http://localhost:5173"
```

Ограничения `ActiveProcessorCount`, heap, metaspace и stack нужны из-за ограничений общего сервера Helios и уменьшают количество создаваемых JVM потоков.

Ограничить доступ к файлу с паролем:

```bash
chmod 700 "$TOMCAT_HOME/bin/setenv.sh"
chmod 600 "$TOMCAT_HOME/conf/worker-service.p12"
```

### 5.3. Деплой worker-service

В чистом Tomcat уже существует демонстрационное приложение `ROOT`. Сохранить его под другим именем и поместить worker-service в корневой context:

```bash
if [ -d "$TOMCAT_HOME/webapps/ROOT" ]; then
  mv "$TOMCAT_HOME/webapps/ROOT" "$TOMCAT_HOME/webapps/tomcat-root-backup"
fi

cp "$APP_HOME/artifacts/worker-service.war" "$TOMCAT_HOME/webapps/ROOT.war"
```

## 6. Сертификат и truststore hr-service

Создать отдельный сертификат WildFly:

```bash
keytool -genkeypair \
  -alias hr-service \
  -keyalg RSA \
  -keysize 2048 \
  -validity 3650 \
  -storetype PKCS12 \
  -keystore "$WILDFLY_HOME/standalone/configuration/hr-service.p12" \
  -storepass changeit \
  -keypass changeit \
  -dname "CN=helios.cs.ifmo.ru, OU=SOA, O=ITMO, C=RU" \
  -ext "SAN=dns:helios.cs.ifmo.ru,ip:127.0.0.1"
```

Создать truststore, через который hr-service будет доверять самоподписанному сертификату worker-service:

```bash
keytool -importcert \
  -noprompt \
  -alias worker-service \
  -file "$APP_HOME/worker-service.crt" \
  -storetype PKCS12 \
  -keystore "$WILDFLY_HOME/standalone/configuration/worker-truststore.p12" \
  -storepass changeit

chmod 600 \
  "$WILDFLY_HOME/standalone/configuration/hr-service.p12" \
  "$WILDFLY_HOME/standalone/configuration/worker-truststore.p12"
```

## 7. Настройка WildFly

### 7.1. Отдельная конфигурация

Не изменять исходный `standalone.xml`. Создать копию:

```bash
cp \
  "$WILDFLY_HOME/standalone/configuration/standalone.xml" \
  "$WILDFLY_HOME/standalone/configuration/standalone-hr.xml"
```

Запустить CLI в embedded-режиме:

```bash
cd "$WILDFLY_HOME"
bin/jboss-cli.sh
```

Выполнить команды последовательно:

```text
embed-server --server-config=standalone-hr.xml

/subsystem=elytron/key-store=hrKS:add(path=hr-service.p12,relative-to=jboss.server.config.dir,credential-reference={clear-text=changeit},type=PKCS12)
/subsystem=elytron/key-manager=hrKM:add(key-store=hrKS,alias-filter=hr-service,credential-reference={clear-text=changeit})
/subsystem=elytron/server-ssl-context=hrSSC:add(key-manager=hrKM,protocols=["TLSv1.2","TLSv1.3"])
/subsystem=undertow/server=default-server/https-listener=https:write-attribute(name=ssl-context,value=hrSSC)

/socket-binding-group=standard-sockets/socket-binding=https:write-attribute(name=port,value=19443)
/socket-binding-group=standard-sockets/socket-binding=management-http:write-attribute(name=port,value=55054)

/subsystem=remoting/http-connector=http-remoting-connector:write-attribute(name=connector-ref,value=https)
/subsystem=undertow/server=default-server/http-listener=default:remove()

/subsystem=io/worker=default:write-attribute(name=io-threads,value=2)
/subsystem=io/worker=default:write-attribute(name=task-core-threads,value=2)
/subsystem=io/worker=default:write-attribute(name=task-max-threads,value=16)

stop-embedded-server
quit
```

Порядок важен: remoting сначала переводится на listener `https`, и только после этого удаляется HTTP listener. Иначе WildFly запускается с ошибкой `jboss.http-upgrade-registry.default`.

Socket binding `http` может оставаться в XML: без Undertow HTTP listener он не открывает сетевой порт.

### 7.2. Память и переменные окружения WildFly

Создать отдельный файл параметров:

```bash
vi "$WILDFLY_HOME/bin/standalone-hr.conf"
```

Содержимое:

```sh
#!/bin/sh

JAVA_OPTS="-Xms64m -Xmx256m -Xss512k -XX:MaxMetaspaceSize=128m -XX:ActiveProcessorCount=2"
JAVA_OPTS="$JAVA_OPTS -Djava.awt.headless=true -Djava.net.preferIPv4Stack=true"
JAVA_OPTS="$JAVA_OPTS -Djava.util.concurrent.ForkJoinPool.common.parallelism=2"
JAVA_OPTS="$JAVA_OPTS -Djavax.net.ssl.trustStore=$HOME/webLab2/wildfly-33.0.2.Final/standalone/configuration/worker-truststore.p12"
JAVA_OPTS="$JAVA_OPTS -Djavax.net.ssl.trustStorePassword=changeit -Djavax.net.ssl.trustStoreType=PKCS12"
export JAVA_OPTS

export WORKER_SERVICE_BASE_URL="https://127.0.0.1:18443"
export WORKER_SERVICE_CONNECT_TIMEOUT="3s"
export WORKER_SERVICE_READ_TIMEOUT="5s"
export HR_CORS_ALLOWED_ORIGINS="https://localhost:19443,https://127.0.0.1:19443,http://localhost:5173"
```

Ограничить доступ:

```bash
chmod 600 "$WILDFLY_HOME/bin/standalone-hr.conf"
```

Не использовать одновременно точные порты из `standalone-hr.xml` и `-Djboss.socket.binding.port-offset`: offset изменит все порты и вместо `19443` получится другое значение.

## 8. Первый запуск worker-service

Для первой проверки запустить Tomcat в foreground:

```bash
cd "$TOMCAT_HOME"
bin/catalina.sh run
```

Успешный запуск содержит сообщение о старте сервера без ошибок deployment. В другом SSH-сеансе проверить:

```bash
curl -ki "https://127.0.0.1:18443/workers?page=1&size=1"
```

После проверки остановить foreground-процесс через `Ctrl+C`, затем запустить в фоне:

```bash
cd "$TOMCAT_HOME"
bin/startup.sh
```

Логи:

```bash
tail -f "$TOMCAT_HOME/logs/catalina.out"
```

## 9. Первый запуск WildFly

Запустить WildFly в foreground:

```bash
cd "$WILDFLY_HOME"
env STANDALONE_CONF="$WILDFLY_HOME/bin/standalone-hr.conf" \
  bin/standalone.sh \
  --server-config=standalone-hr.xml \
  -b=0.0.0.0 \
  -bmanagement=127.0.0.1
```

Не добавлять `port-offset`.

В другом SSH-сеансе подключиться к CLI:

```bash
cd "$WILDFLY_HOME"
bin/jboss-cli.sh --connect --controller=127.0.0.1:55054
```

Задеплоить hr-service как корневое приложение:

```text
deploy /home/studs/s413041/webLab2/artifacts/hr-service.war --name=ROOT.war --runtime-name=ROOT.war --force
deployment-info
quit
```

Ожидаемые состояния `ROOT.war`: `OK` и `ENABLED`.

После первой проверки остановить foreground-процесс через `Ctrl+C` и запустить WildFly в фоне:

```bash
cd "$WILDFLY_HOME"
nohup env STANDALONE_CONF="$WILDFLY_HOME/bin/standalone-hr.conf" \
  bin/standalone.sh \
  --server-config=standalone-hr.xml \
  -b=0.0.0.0 \
  -bmanagement=127.0.0.1 \
  > "$APP_HOME/logs/wildfly-console.log" 2>&1 &
```

Логи:

```bash
tail -f "$APP_HOME/logs/wildfly-console.log"
```

## 10. Проверка на Helios

Проверить открытые Java-порты:

```bash
sockstat -4 -l | grep java
```

Должны присутствовать:

- `*:18443` — worker-service;
- `*:19443` — hr-service и frontend;
- `127.0.0.1:55054` — WildFly management.

HTTP-порты приложений `8080` и `8081` слушаться не должны.

Проверить процессы и реальные JVM-параметры:

```bash
jps -lv
```

Процесс `Jps` в выводе является временной утилитой самой команды и сразу завершается.

Проверить worker-service:

```bash
curl -ki "https://127.0.0.1:18443/workers?page=1&size=1"
```

Проверить hr-service и встроенный frontend:

```bash
curl -ki https://127.0.0.1:19443/openapi/hr-service.yaml
curl -ki https://127.0.0.1:19443/
```

Проверить вызов worker-service из hr-service без изменения данных:

```bash
curl -ki -X POST \
  https://127.0.0.1:19443/hr/index/2147483647/1.1
```

Ожидается HTTP `422`. Если возвращается `503`, следует проверить truststore и доступность worker-service.

Проверить CORS worker-service:

```bash
curl -ksi \
  -H "Origin: https://localhost:19443" \
  "https://127.0.0.1:18443/workers?page=1&size=1" \
  | grep -i access-control
```

Ожидаемый заголовок:

```text
Access-Control-Allow-Origin: https://localhost:19443
```

## 11. Доступ из браузера через SSH-туннели

На локальном компьютере:

```bash
ssh \
  -L 18443:127.0.0.1:18443 \
  -L 19443:127.0.0.1:19443 \
  s413041@helios
```

Терминал с туннелем должен оставаться открытым.

Сначала открыть в браузере worker-service и вручную принять его самоподписанный сертификат:

```text
https://localhost:18443/openapi/worker-service.yaml
```

После отображения YAML открыть frontend:

```text
https://localhost:19443/
```

Для сертификата WildFly также потребуется вручную подтвердить доверие. В настройках frontend должны быть адреса:

```text
worker-service: https://localhost:18443
hr-service:     https://localhost:19443
```

`curl -k` игнорирует недоверенный сертификат, а браузерный `fetch` этого делать не может. Поэтому ошибка `ERR_CERT_AUTHORITY_INVALID` устраняется принятием сертификата worker-service в самом браузере либо добавлением сертификата в локальное доверенное хранилище.

## 12. Повторный деплой

Локально пересобрать приложения:

```bash
./gradlew :worker-service:bootWar :hr-service:bootWar
```

Передать новые WAR:

```bash
scp worker-service/build/libs/worker-service.war \
  s413041@helios:/home/studs/s413041/webLab2/artifacts/

scp hr-service/build/libs/hr-service.war \
  s413041@helios:/home/studs/s413041/webLab2/artifacts/
```

Worker-service обновляется после остановки Tomcat:

```bash
"$TOMCAT_HOME/bin/shutdown.sh"
cp "$APP_HOME/artifacts/worker-service.war" "$TOMCAT_HOME/webapps/ROOT.war"
"$TOMCAT_HOME/bin/startup.sh"
```

Hr-service обновляется без перезапуска WildFly:

```bash
cd "$WILDFLY_HOME"
bin/jboss-cli.sh \
  --connect \
  --controller=127.0.0.1:55054 \
  --command="deploy /home/studs/s413041/webLab2/artifacts/hr-service.war --name=ROOT.war --runtime-name=ROOT.war --force"
```

После обновления frontend выполнить жёсткую перезагрузку страницы: `Cmd+Shift+R` или `Ctrl+Shift+R`.

## 13. Остановка

Tomcat:

```bash
"$TOMCAT_HOME/bin/shutdown.sh"
```

WildFly:

```bash
"$WILDFLY_HOME/bin/jboss-cli.sh" \
  --connect \
  --controller=127.0.0.1:55054 \
  --command=":shutdown"
```

Проверка оставшихся JVM:

```bash
jps -lv
```

Если штатная остановка не сработала, определить PID нужного процесса через `jps -lv` и отправить обычный `TERM`:

```bash
kill <PID>
```

`kill -9` следует использовать только в крайнем случае.

## 14. Диагностика нехватки native threads

Проверить количество потоков конкретной JVM:

```bash
ps -M <PID> | wc -l
```

Проверить ограничения пользователя:

```bash
limits -a
```

Проверить память и процессы:

```bash
top
```

Ошибка `unable to create native thread` не обязательно означает нехватку Java heap. На Helios она также возникает при слишком большом числе JVM-потоков или одновременном запуске нескольких экземпляров WildFly. Поэтому конфигурация ограничивает `ActiveProcessorCount`, Undertow/XNIO worker и размер stack каждого потока.

## Справочные материалы

- [Tomcat 10.1: SSL/TLS Configuration](https://tomcat.apache.org/tomcat-10.1-doc/ssl-howto.html)
- [Tomcat 10.1: HTTP Connector](https://tomcat.apache.org/tomcat-10.1-doc/config/http.html)
- [WildFly Elytron Security Guide](https://docs.wildfly.org/28/WildFly_Elytron_Security.html)
- [WildFly 33 documentation](https://docs.wildfly.org/33/)

## Вариант задания

![Вариант лабораторной работы №1](variants/soa%20lab-1%20var.png)

![Вариант лабораторной работы №2](variants/soa%20lab-2%20var.jpg)
