FROM openjdk:8

EXPOSE 8080

ARG WORKDIR
ARG VERSION
ARG JAVAOPTS
ARG SETTINGS

COPY hercules-graphite-sink/target/hercules-graphite-sink-${VERSION}.jar ${WORKDIR}/hercules-graphite-sink-${VERSION}.jar

ENTRYPOINT java ${JAVAOPTS} -jar hercules-graphite-sink-${VERSION}.jar application.properties=${SETTINGS} 

