FROM openjdk:8

EXPOSE 8080

ARG WORKDIR="usr/src"
ARG VERSION="unknown"

ENV VERSION=${VERSION}
ENV JAVAOPTS=""
ENV SETTINGS="/etc/hercules-elastic-sink/properties"

COPY hercules-elastic-sink/target/hercules-elastic-sink-${VERSION}.jar ${WORKDIR}/hercules-elastic-sink-${VERSION}.jar

WORKDIR ${WORKDIR}

ENTRYPOINT java ${JAVAOPTS} -jar hercules-elastic-sink-${VERSION}.jar application.properties=${SETTINGS} 

