FROM openjdk:8

EXPOSE 8080

ARG WORKDIR="usr/src"
ARG VERSION="unknown"

ENV VERSION=${VERSION}
ENV JAVAOPTS=""
ENV SETTINGS="/etc/hercules-stream-api/properties"

COPY hercules-stream-api/target/hercules-stream-api-${VERSION}.jar ${WORKDIR}/hercules-stream-api-${VERSION}.jar

WORKDIR ${WORKDIR}

ENTRYPOINT java ${JAVAOPTS} -jar hercules-stream-api-${VERSION}.jar application.properties=${SETTINGS} 

