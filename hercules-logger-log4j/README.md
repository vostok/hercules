# Log4j logger

## Описание 
Клиентская библиотека для отправки логов в Hercules Gateway.

## Параметры запуска
Встраивается в pom.xml
```xml
<dependency>
    <groupId>ru.kontur.vostok.hercules</groupId>
    <artifactId>hercules-logger-log4j</artifactId>
    <version>{version}</version>
</dependency>
```

## Файлы настроек
Имеет в качестве зависимости [gateway client](https://git.skbkontur.ru/kgn/hercules/tree/master/hercules-gateway-client), а значит и его конфигурационные файлы.

Пример log4j2.properties
```properties
appenders = console, http
 
appender.console.type = Console
appender.console.name = STDOUT
appender.console.layout.type = PatternLayout
appender.console.layout.pattern = %m%n
 
appender.http.type = Log4jHttpAppender
appender.http.name = HTTP
appender.http.stream = stream_test
appender.http.queueName = main
 
 
rootLogger.level = info
rootLogger.appenderRefs = console, http
rootLogger.appenderRef.console.ref = STDOUT
rootLogger.appenderRef.http.ref = HTTP
```