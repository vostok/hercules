# Logback logger

## Описание 
Клиентская библиотека для отправки логов в Hercules Gateway.

## Параметры запуска
Встраивается в pom.xml
```xml
<dependency>
    <groupId>ru.kontur.vostok.hercules</groupId>
    <artifactId>hercules-logger-logback</artifactId>
    <version>{version}</version>
</dependency>
```

## Файлы настроек
Имеет в качестве зависимости [gateway client](https://git.skbkontur.ru/kgn/hercules/tree/master/hercules-gateway-client), а значит и его конфигурационные файлы.

Пример logback.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <shutdownHook class="ch.qos.logback.core.hook.DelayingShutdownHook"/>
 
    <appender name="consoleAppender" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <Pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg %n
            </Pattern>
        </encoder>
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>TRACE</level>
        </filter>
    </appender>
 
    <appender name="httpAppender" class="ru.kontur.vostok.hercules.logger.logback.LogbackHttpAppender">
        <queueName>main</queueName>
        <stream>stream_test</stream>
    </appender>
 
    <root>
        <level value="INFO"/>
        <appender-ref ref="httpAppender"/>
        <appender-ref ref="consoleAppender"/>
    </root>
</configuration>
```