FROM openjdk:8

EXPOSE 8080

ARG WORKDIR="usr/src"
ARG VERSION="unknown"

ENV VERSION=${VERSION}
ENV JAVAOPTS=""
ENV SETTINGS="/etc/hercules-gate/properties"

COPY hercules-gate/target/hercules-gate-${VERSION}.jar ${WORKDIR}/hercules-gate-${VERSION}.jar

WORKDIR ${WORKDIR}

ENTRYPOINT java ${JAVAOPTS} -jar hercules-gate-${VERSION}.jar application.properties=${SETTINGS} 

