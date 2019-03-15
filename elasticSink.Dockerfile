FROM openjdk:8

EXPOSE 8080

ARG WORKDIR
ARG VERSION
ARG JAVAOPTS
ARG SETTINGS

COPY hercules-elastic-sink/target/hercules-elastic-sink-${VERSION}.jar ${WORKDIR}/hercules-elastic-sink-${VERSION}.jar

ENTRYPOINT java ${JAVAOPTS} -jar hercules-elastic-sink-${VERSION}.jar application.properties=${SETTINGS} 

