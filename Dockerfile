FROM bellsoft/liberica-openjre-alpine:11.0.12-7

RUN apk add --no-cache jattach --repository http://dl-cdn.alpinelinux.org/alpine/edge/community/

ARG SERVICENAME="unknown"
ARG VERSION="unknown"
ARG WORKDIR="/usr/lib/hercules/${SERVICENAME}"

COPY ${SERVICENAME}/target/${SERVICENAME}-${VERSION}.jar ${WORKDIR}/app.jar
COPY ${SERVICENAME}/application.properties /etc/hercules/application.properties

WORKDIR ${WORKDIR}

ENTRYPOINT java $JAVA_OPTIONS -jar app.jar application.properties=file:///etc/hercules/application.properties
