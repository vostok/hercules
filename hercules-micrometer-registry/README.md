# Micrometer Registry

## Описание 
Клиентская библиотека для отправки метрик в Hercules Gateway.

## Параметры запуска
Встраивается в pom.xml
```xml
<dependency>
    <groupId>ru.kontur.vostok.hercules</groupId>
    <artifactId>hercules-micrometer-registry</artifactId>
    <version>{version}</version>
</dependency>
```

Конфиги настраиваются вручную. Для автоматической настройки существует класс `HerculesMetricConfigFactory`. В нем представлены несколько способов загрузки конфигурационных файлов:
- `HerculesMetricConfigFactory.fromResource(String resourceName)` - подгружает из ресурсов с именем `resourceName`
- `HerculesMetricConfigFactory.fromResource()` - подгружает из ресурсов с именем _hercules-metric.properties_
- `HerculesMetricConfigFactory.fromFile(String filename)` - подгружает файл с именем `filename`
- `HerculesMetricConfigFactory.fromFile()` - подгружает файл с именем, находящимся в системном свойстве `-Dgateway.client.config` 

## Файлы настроек
Имеет в качестве зависимости [gateway client](https://git.skbkontur.ru/kgn/hercules/tree/master/hercules-gateway-client), а значит и его конфигурационные файлы.

Пример hercules-metric.properties
```properties
hercules.metrics.step=PT3S
hercules.metrics.queue.name=name
hercules.metrics.queue.stream=stream_test
hercules.metrics.queue.periodMillis=1000
hercules.metrics.queue.batchSize=100
hercules.metrics.queue.capacity=10000000
hercules.metrics.queue.loseOnOverflow=false
```

Обязаьельно!
1. Каждая общая настройка внутри конфигурационного файла должна иметь префикс hercules.metrics
2. Каждая настройка очереди событий внутри конфигурационного файла должна иметь префикс hercules.metrics.queue