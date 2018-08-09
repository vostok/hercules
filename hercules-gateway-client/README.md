# Gateway Client

## Описание 
Клиентская библиотека для формирования пачки событий и отправки в Hercules Gateway.

## Параметры запуска
Встраивается в pom.xml
```xml
<dependency>
    <groupId>ru.kontur.vostok.hercules</groupId>
    <artifactId>hercules-gateway-client</artifactId>
    <version>{version}</version>
</dependency>
```

## Файлы настроек
Пытается получить путь до файла через системное свойство `-Dgateway.client.config`, иначе пытается достать файл конфигурации в ресурсах с названием `gateway-client.properties`

gateway-client.properties
```properties
url=http://localhost:6306
apiKey=test-api-key
project=test_project
env=test
```
