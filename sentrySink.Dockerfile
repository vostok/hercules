FROM openjdk:8

EXPOSE 8080

ARG WORKDIR="usr/src"
ARG VERSION="unknown"

ENV VERSION=${VERSION}
ENV JAVAOPTS=""
ENV SETTINGS="/etc/hercules-sentry-sink/properties"

COPY hercules-sentry-sink/target/hercules-sentry-sink-${VERSION}.jar ${WORKDIR}/hercules-sentry-sink-${VERSION}.jar

WORKDIR ${WORKDIR}

ENTRYPOINT java ${JAVAOPTS} -jar hercules-sentry-sink-${VERSION}.jar application.properties=${SETTINGS} 

