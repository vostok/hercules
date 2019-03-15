FROM openjdk:8

EXPOSE 8080

ARG WORKDIR
ARG VERSION
ARG JAVAOPTS
ARG SETTINGS

COPY hercules-stream-sink/target/hercules-stream-sink-${VERSION}.jar ${WORKDIR}/hercules-stream-sink-${VERSION}.jar

ENTRYPOINT java ${JAVAOPTS} -jar hercules-stream-sink-${VERSION}.jar application.properties=${SETTINGS} 

