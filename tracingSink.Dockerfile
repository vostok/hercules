FROM openjdk:8

EXPOSE 8080

ARG WORKDIR
ARG VERSION
ARG JAVAOPTS
ARG SETTINGS

COPY hercules-tracing-sink/target/hercules-tracing-sink-${VERSION}.jar ${WORKDIR}/hercules-tracing-sink-${VERSION}.jar

ENTRYPOINT java ${JAVAOPTS} -jar hercules-tracing-sink-${VERSION}.jar application.properties=${SETTINGS} 

