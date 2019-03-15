FROM openjdk:8

EXPOSE 8080

ARG WORKDIR
ARG VERSION
ARG JAVAOPTS
ARG SETTINGS

COPY hercules-gate/target/hercules-gate-${VERSION}.jar ${WORKDIR}/hercules-gate-${VERSION}.jar

ENTRYPOINT java ${JAVAOPTS} -jar hercules-gate-${VERSION}.jar application.properties=${SETTINGS} 

