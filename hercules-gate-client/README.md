# Gateway Client

## Описание 
Клиентская библиотека для формирования пачки событий и отправки в Hercules Gateway.

## Параметры запуска
Встраивается в pom.xml
```xml
<dependency>
    <groupId>ru.kontur.vostok.hercules</groupId>
    <artifactId>hercules-gate-client</artifactId>
    <version>{version}</version>
</dependency>
```

## Файлы настроек
Пытается получить путь до файла через системное свойство `-Dhercules.gate.client.config`, иначе пытается достать файл конфигурации в ресурсах с названием `hercules-gate-client.properties`

hercules-gate-client.properties
```properties
url=http://localhost:6306
apiKey=test-api-key
project=test_project
env=test
```
