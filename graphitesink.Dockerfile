FROM openjdk:8

EXPOSE 8080

ARG WORKDIR="usr/src"
ARG VERSION="unknown"

ENV VERSION=${VERSION}
ENV JAVAOPTS=""
ENV SETTINGS="/etc/hercules-graphite-sink/properties"

RUN useradd packer

COPY hercules-graphite-sink/target/hercules-graphite-sink-${VERSION}.jar ${WORKDIR}/hercules-graphite-sink-${VERSION}.jar

WORKDIR ${WORKDIR}

RUN chown -R packer ${WORKDIR}

USER packer

ENTRYPOINT java ${JAVAOPTS} -jar hercules-graphite-sink-${VERSION}.jar application.properties=${SETTINGS} 

