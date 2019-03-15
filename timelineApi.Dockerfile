FROM openjdk:8

EXPOSE 8080

ARG WORKDIR
ARG VERSION
ARG JAVAOPTS
ARG SETTINGS

COPY hercules-timeline-api/target/hercules-timeline-api-${VERSION}.jar ${WORKDIR}/hercules-timeline-api-${VERSION}.jar

ENTRYPOINT java ${JAVAOPTS} -jar hercules-timeline-api-${VERSION}.jar application.properties=${SETTINGS} 

