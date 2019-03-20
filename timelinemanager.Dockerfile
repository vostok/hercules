FROM openjdk:8

EXPOSE 8080

ARG WORKDIR="usr/src"
ARG VERSION="unknown"

ENV VERSION=${VERSION}
ENV JAVAOPTS=""
ENV SETTINGS="/etc/hercules-timeline-manager/properties"

RUN useradd packer

COPY hercules-timeline-manager/target/hercules-timeline-manager-${VERSION}.jar ${WORKDIR}/hercules-timeline-manager-${VERSION}.jar

WORKDIR ${WORKDIR}

RUN chown -R packer ${WORKDIR}

USER packer

ENTRYPOINT java ${JAVAOPTS} -jar hercules-timeline-manager-${VERSION}.jar application.properties=${SETTINGS} 

