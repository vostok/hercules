FROM openjdk:8

EXPOSE 8080

ARG WORKDIR
ARG VERSION
ARG JAVAOPTS
ARG SETTINGS

COPY hercules-management-api/target/hercules-management-api-${VERSION}.jar ${WORKDIR}/hercules-management-api-${VERSION}.jar

ENTRYPOINT java ${JAVAOPTS} -jar hercules-management-api-${VERSION}.jar application.properties=${SETTINGS} 

