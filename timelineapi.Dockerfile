FROM openjdk:8

EXPOSE 8080

ARG WORKDIR="usr/src"
ARG VERSION="unknown"

ENV VERSION=${VERSION}
ENV JAVAOPTS=""
ENV SETTINGS="/etc/hercules-timeline-api/properties"

COPY hercules-timeline-api/target/hercules-timeline-api-${VERSION}.jar ${WORKDIR}/hercules-timeline-api-${VERSION}.jar

WORKDIR ${WORKDIR}

ENTRYPOINT java ${JAVAOPTS} -jar hercules-timeline-api-${VERSION}.jar application.properties=${SETTINGS} 

