FROM openjdk:8-jre-alpine

ARG PROJECTNAME="unknown"
ARG VERSION="unknown"

WORKDIR "/usr/lib/hercules-elastic-sink"

ENV VERSION=${VERSION}
ENV JAVAOPTS=""
ENV SETTINGS="/etc/hercules-elastic-sink/properties"

COPY ${PROJECTNAME}/target/${PROJECTNAME}-${VERSION}.jar ${WORKDIR}/${PROJECTNAME}-${VERSION}.jar

ENTRYPOINT java ${JAVAOPTS} -jar ${PROJECTNAME}-${VERSION}.jar application.properties=${SETTINGS} 

