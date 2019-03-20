FROM openjdk:8

EXPOSE 8080

ARG WORKDIR="usr/src"
ARG VERSION="unknown"

ENV VERSION=${VERSION}
ENV JAVAOPTS=""
ENV SETTINGS="/etc/hercules-timeline-sink/properties"

RUN useradd packer

COPY hercules-timeline-sink/target/hercules-timeline-sink-${VERSION}.jar ${WORKDIR}/hercules-timeline-sink-${VERSION}.jar

WORKDIR ${WORKDIR}

RUN chown -R packer ${WORKDIR}

USER packer

ENTRYPOINT java ${JAVAOPTS} -jar hercules-timeline-sink-${VERSION}.jar application.properties=${SETTINGS} 

