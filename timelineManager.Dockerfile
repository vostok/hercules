FROM openjdk:8

EXPOSE 8080

ARG WORKDIR
ARG VERSION
ARG JAVAOPTS
ARG SETTINGS

COPY hercules-timeline-manager/target/hercules-timeline-manager-${VERSION}.jar ${WORKDIR}/hercules-timeline-manager-${VERSION}.jar

ENTRYPOINT java ${JAVAOPTS} -jar hercules-timeline-manager-${VERSION}.jar application.properties=${SETTINGS} 

