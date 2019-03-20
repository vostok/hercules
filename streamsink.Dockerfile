FROM openjdk:8

EXPOSE 8080

ARG WORKDIR="usr/src"
ARG VERSION="unknown"

ENV VERSION=${VERSION}
ENV JAVAOPTS=""
ENV SETTINGS="/etc/hercules-stream-sink/properties"

RUN useradd packer

COPY hercules-stream-sink/target/hercules-stream-sink-${VERSION}.jar ${WORKDIR}/hercules-stream-sink-${VERSION}.jar

WORKDIR ${WORKDIR}

RUN chown -R packer ${WORKDIR}

USER packer

ENTRYPOINT java ${JAVAOPTS} -jar hercules-stream-sink-${VERSION}.jar application.properties=${SETTINGS} 

