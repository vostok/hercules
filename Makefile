MAVENIMAGENAME  := maven
JAVAIMAGENAME  := java
VERSION := 0.20.0-SNAPSHOT

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
	@docker build --no-cache --build-arg VERSION=${VERSION} --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t $(JAVAIMAGENAME) -f $(ELASTICSINKBUILDERFILE) .
	@docker tag java ${ELASTICSINKIMAGE}
	@docker push ${ELASTICSINKIMAGE}
.PHONY: pushelasticsink

pushgate:
	@docker build --no-cache --build-arg VERSION=${VERSION} --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t $(JAVAIMAGENAME) -f $(GATEBUILDERFILE) .
	@docker tag java ${GATEIMAGE}
	@docker push ${GATEIMAGE}
.PHONY: pushgate

pushgraphitesink:
	@docker build --no-cache --build-arg VERSION=${VERSION} --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t $(JAVAIMAGENAME) -f $(GRAPHITESINKBUILDERFILE) .
	@docker tag java ${GRAPHITESINKIMAGE}
	@docker push ${GRAPHITESINKIMAGE}
.PHONY: pushgraphitesink

pushmanagementapi:
	@docker build --no-cache --build-arg VERSION=${VERSION} --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t $(JAVAIMAGENAME) -f $(MANAGEMENTAPIBUILDERFILE) .
	@docker tag java ${MANAGEMENTAPIIMAGE}
	@docker push ${MANAGEMENTAPIIMAGE}
.PHONY: pushmanagementapi

pushsentrysink:
	@docker build --no-cache --build-arg VERSION=${VERSION} --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t $(JAVAIMAGENAME) -f $(SENTRYSINKBUILDERFILE) .
	@docker tag java ${SENTRYSINKIMAGE}
	@docker push ${SENTRYSINKIMAGE}
.PHONY: pushsentrysink

pushstreamapi:
	@docker build --no-cache --build-arg VERSION=${VERSION} --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t $(JAVAIMAGENAME) -f $(STREAMAPIBUILDERFILE) .
	@docker tag java ${STREAMAPIIMAGE}
	@docker push ${STREAMAPIIMAGE}
.PHONY: pushstreamapi

pushatreammanager:
	@docker build --no-cache --build-arg VERSION=${VERSION} --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t $(JAVAIMAGENAME) -f $(STREAMMANAGERBUILDERFILE) .
	@docker tag java ${STREAMMANAGERIMAGE}
	@docker push ${STREAMMANAGERIMAGE}
.PHONY: pushatreammanager

pushstreamsink:
	@docker build --no-cache --build-arg VERSION=${VERSION} --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t $(JAVAIMAGENAME) -f $(STREAMSINKBUILDERFILE) .
	@docker tag java ${STREAMSINKIMAGE}
	@docker push ${STREAMSINKIMAGE}
.PHONY: pushstreamsink

pushtimelineapi:
	@docker build --no-cache --build-arg VERSION=${VERSION} --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t $(JAVAIMAGENAME) -f $(TIMELINEAPIBUILDERFILE) .
	@docker tag java ${TIMELINEAPIIMAGE}
	@docker push ${TIMELINEAPIIMAGE}
.PHONY: pushtimelineapi

pushtimelinemanager:
	@docker build --no-cache --build-arg VERSION=${VERSION} --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t $(JAVAIMAGENAME) -f $(TIMELINEMANAGERBUILDERFILE) .
	@docker tag java ${TIMELINEMANAGERIMAGE}
	@docker push ${TIMELINEMANAGERIMAGE}
.PHONY: pushtimelinemanager

pushtracingsink:
	@docker build --no-cache --build-arg VERSION=${VERSION} --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t $(JAVAIMAGENAME) -f $(TRACINGSINKBUILDERFILE) .
	@docker tag java ${TRACINGSINKIMAGE}
	@docker push ${TRACINGSINKIMAGE}
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


