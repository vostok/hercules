FROM openjdk:8-jre-alpine

ARG SERVICENAME="unknown"
ARG VERSION="unknown"

WORKDIR "/usr/lib/${SERVICENAME}"

ENV VERSION=${VERSION}
ENV JAVAOPTS=""
ENV SETTINGS="/etc/${SERVICENAME}"

COPY ${SERVICENAME}/target/${SERVICENAME}-${VERSION}.jar ${WORKDIR}/app.jar

ENTRYPOINT java ${JAVAOPTS} -jar app.jar application.properties=${SETTINGS} 

