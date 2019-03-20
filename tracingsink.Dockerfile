FROM openjdk:8

EXPOSE 8080

ARG WORKDIR="usr/src"
ARG VERSION="unknown"

ENV VERSION=${VERSION}
ENV JAVAOPTS=""
ENV SETTINGS="/etc/hercules-tracing-sink/properties"

RUN useradd packer

COPY hercules-tracing-sink/target/hercules-tracing-sink-${VERSION}.jar ${WORKDIR}/hercules-tracing-sink-${VERSION}.jar

WORKDIR ${WORKDIR}

RUN chown -R packer ${WORKDIR}

USER packer

ENTRYPOINT java ${JAVAOPTS} -jar hercules-tracing-sink-${VERSION}.jar application.properties=${SETTINGS} 

