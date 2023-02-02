# Hercules Application

Core application-required classes.

### Application class properties

`application.host` - server host, default value: `0.0.0.0` (deprecated use `http.server.host` instead)

`application.port` - server port, default value: `8080` (deprecated use `http.server.port` instead)

`application.shutdown.grace.period.ms` - graceful shutdown time budget in milliseconds, if value is `0` then the `Application.gracefulShutdown` method will never be executed, default value `0`   

`application.shutdown.timeout.ms` - hard shutdown time budget in milliseconds, default value `5000` 

`application.state.observers.classes` - classes names of implementation of `ApplicationStateListener` interface

Example configuration in .properties file syntax:
```properties
application.host=0.0.0.0
application.port=8080
application.shutdown.grace.period.ms=15000
application.shutdown.timeout.ms=10000
application.state.observers.classes=ru.kontur.MyFirstStateObserver,ru.kontur.MySecondStateObserver
```
