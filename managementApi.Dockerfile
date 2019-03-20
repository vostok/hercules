FROM openjdk:8

EXPOSE 8080

ARG WORKDIR="usr/src"
ARG VERSION="unknown"

ENV VERSION=${VERSION}
ENV JAVAOPTS=""
ENV SETTINGS="/etc/hercules-management-api/properties"

COPY hercules-management-api/target/hercules-management-api-${VERSION}.jar ${WORKDIR}/hercules-management-api-${VERSION}.jar

WORKDIR ${WORKDIR}

ENTRYPOINT java ${JAVAOPTS} -jar hercules-management-api-${VERSION}.jar application.properties=${SETTINGS} 

