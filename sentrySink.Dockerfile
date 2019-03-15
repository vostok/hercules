FROM openjdk:8

EXPOSE 8080

ARG WORKDIR
ARG VERSION
ARG JAVAOPTS
ARG SETTINGS

COPY hercules-sentry-sink/target/hercules-sentry-sink-${VERSION}.jar ${WORKDIR}/hercules-sentry-sink-${VERSION}.jar

ENTRYPOINT java ${JAVAOPTS} -jar hercules-sentry-sink-${VERSION}.jar application.properties=${SETTINGS} 

