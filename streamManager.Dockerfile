FROM openjdk:8

EXPOSE 8080

ARG WORKDIR
ARG VERSION
ARG JAVAOPTS
ARG SETTINGS

COPY hercules-stream-api/target/hercules-stream-manager-${VERSION}.jar ${WORKDIR}/hercules-stream-manager-${VERSION}.jar

ENTRYPOINT java ${JAVAOPTS} -jar hercules-stream-manager-${VERSION}.jar application.properties=${SETTINGS} 

