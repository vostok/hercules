FROM openjdk:8-jre-alpine

ARG SERVICENAME="unknown"
ARG VERSION="unknown"
ARG WORKDIR="/usr/lib/hercules/${SERVICENAME}"

COPY ${SERVICENAME}/target/${SERVICENAME}-${VERSION}.jar ${WORKDIR}/app.jar
COPY ${SERVICENAME}/application.properties /etc/hercules/application.properties

WORKDIR ${WORKDIR}

ENTRYPOINT java $JAVA_OPTIONS -jar app.jar application.properties=file:///etc/hercules/application.properties
