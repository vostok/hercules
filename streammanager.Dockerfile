FROM openjdk:8

EXPOSE 8080

ARG WORKDIR="usr/src"
ARG VERSION="unknown"

ENV VERSION=${VERSION}
ENV JAVAOPTS=""
ENV SETTINGS="/etc/hercules-stream-manager/properties"

COPY hercules-stream-manager/target/hercules-stream-manager-${VERSION}.jar ${WORKDIR}/hercules-stream-manager-${VERSION}.jar

WORKDIR ${WORKDIR}

ENTRYPOINT java ${JAVAOPTS} -jar hercules-stream-manager-${VERSION}.jar application.properties=${SETTINGS} 

