MAVENIMAGENAME  := maven
JAVAIMAGENAME  := java
VERSION := 0.20.0-SNAPSHOT
IAMGE_NAME := tsypaev/elasticsink

JAVAOPTS := ""
SETTINGS := ""
WORKDIR := /tmp

ELASTICSINKBUILDERFILE := elasticSink.Dockerfile
GATEBUILDERFILE := gate.Dockerfile
GRAPHITESINKBUILDERFILE := graphiteSink.Dockerfile
MANAGEMENTAPIBUILDERFILE := managementApi.Dockerfile
SENTRYSINKBUILDERFILE := sentrySink.Dockerfile
STREAMAPIBUILDERFILE := streamApi.Dockerfile
STREAMMANAGERBUILDERFILE := streamManager.Dockerfile
STREAMSINKBUILDERFILE := streamSink.Dockerfile
TIMELINEAPIBUILDERFILE := timelineApi.Dockerfile
TIMELINEMANAGERBUILDERFILE := timelineManager.Dockerfile
TRACINGSINKBUILDERFILE := tracingSink.Dockerfile

ELASTICSINKIMAGE := tsypaev/elasticSink
GATEIMAGE := tsypaev/gate
GRAPHITESINKIMAGE := tsypaev/graphiteSink
MANAGEMENTAPIIMAGE := tsypaev/managementApi
SENTRYSINKIMAGE := tsypaev/sentrySink
STREAMAPIIMAGE := tsypaev/streamApi
STREAMMANAGERIMAGE := tsypaev/streamManager
STREAMSINKIMAGE := tsypaev/tsypaev/streamSink
TIMELINEAPIIMAGE := tsypaev/timelineApi
TIMELINEMANAGERIMAGE := tsypaev/timelineManager
TRACINGSINKIMAGE := tsypaev/tracingSink

pushelasticsink:
	@docker build --no-cache --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t $(JAVAIMAGENAME) -f $(ELASTICSINKBUILDERFILE) .
	@docker tag java ${IMAGENAME}
	@docker push ${IMAGENAME}
.PHONY: pushelasticsink

pushgate:
	@docker build --no-cache --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t $(JAVAIMAGENAME) -f $(GATEBUILDERFILE) .
	@docker tag java ${IMAGENAME}
	@docker push ${IMAGENAME}
.PHONY: pushgate

pushgraphitesink:
	@docker build --no-cache --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t $(JAVAIMAGENAME) -f $(GRAPHITESINKBUILDERFILE) .
	@docker tag java ${IMAGENAME}
	@docker push ${IMAGENAME}
.PHONY: pushgraphitesink

pushmanagementapi:
	@docker build --no-cache --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t $(JAVAIMAGENAME) -f $(MANAGEMENTAPIBUILDERFILE) .
	@docker tag java ${IMAGENAME}
	@docker push ${IMAGENAME}
.PHONY: pushmanagementapi

pushsentrysink:
	@docker build --no-cache --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t $(JAVAIMAGENAME) -f $(SENTRYSINKBUILDERFILE) .
	@docker tag java ${IMAGENAME}
	@docker push ${IMAGENAME}
.PHONY: pushsentrysink

pushstreamapi:
	@docker build --no-cache --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t $(JAVAIMAGENAME) -f $(STREAMAPIBUILDERFILE) .
	@docker tag java ${IMAGENAME}
	@docker push ${IMAGENAME}
.PHONY: pushstreamapi

pushatreammanager:
	@docker build --no-cache --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t $(JAVAIMAGENAME) -f $(STREAMMANAGERBUILDERFILE) .
	@docker tag java ${IMAGENAME}
	@docker push ${IMAGENAME}
.PHONY: pushatreammanager

pushstreamsink:
	@docker build --no-cache --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t $(JAVAIMAGENAME) -f $(STREAMSINKBUILDERFILE) .
	@docker tag java ${IMAGENAME}
	@docker push ${IMAGENAME}
.PHONY: pushstreamsink

pushtimelineapi:
	@docker build --no-cache --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t $(JAVAIMAGENAME) -f $(TIMELINEAPIBUILDERFILE) .
	@docker tag java ${IMAGENAME}
	@docker push ${IMAGENAME}
.PHONY: pushtimelineapi

pushtimelinemanager:
	@docker build --no-cache --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t $(JAVAIMAGENAME) -f $(TIMELINEMANAGERBUILDERFILE) .
	@docker tag java ${IMAGENAME}
	@docker push ${IMAGENAME}
.PHONY: pushtimelinemanager

pushtracingsink:
	@docker build --no-cache --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t $(JAVAIMAGENAME) -f $(TRACINGSINKBUILDERFILE) .
	@docker tag java ${IMAGENAME}
	@docker push ${IMAGENAME}
.PHONY: pushtracingsink

pushallimages:
	make pushelasticsink
	make pushgate
	make pushgraphitesink
	make pushmanagementapi
	make pushsentrysink
	make pushstreamapi
	make pushatreammanager
	make pushstreamsink
	make pushtimelineapi
	make pushtimelinemanager
	make pushtracingsink
.PHONY: pushallimages


