FROM openjdk:8-jre-alpine

WORKDIR "/usr/lib/hercules-elastic-sink"
ARG VERSION="unknown"

ENV VERSION=${VERSION}
ENV JAVAOPTS=""
ENV SETTINGS="/etc/hercules-elastic-sink/properties"

COPY hercules-elastic-sink/target/hercules-elastic-sink-${VERSION}.jar ${WORKDIR}/hercules-elastic-sink-${VERSION}.jar

ENTRYPOINT java ${JAVAOPTS} -jar hercules-elastic-sink-${VERSION}.jar application.properties=${SETTINGS} 

