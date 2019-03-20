MAVENIMAGENAME  := maven
VERSION := 0.20.0-SNAPSHOT

JAVAOPTS := ""
SETTINGS := ""
WORKDIR := /tmp
PREFIX := tsypaev/

ELASTICSINK := elasticsink
GATE := gate
GRAPHITESINK := graphitesink
MANAGEMENTAPI := managementapi
SENTRYSINK := sentrysink
STREAMAPI := streamapi
STREAMMANAGER := streammanager
STREAMSINK := streamsink
TIMELINEAPI := timelineapi
TIMELINEMANAGER := timelinemanager
TIMELINESINK := timelinesink
TRACINGSINK := tracingsink

.PHONY: pushallimages
pushallimages: pushelasticsink pushgate pushgraphitesink pushmanagementapi pushsentrysink pushstreamapi pushatreammanager pushstreamsink pushtimelineapi pushtimelinemanager pushtimelinesink pushtracingsink

pushelasticsink:
	@docker build --no-cache --build-arg VERSION=${VERSION} --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t ${PREFIX}${ELASTICSINK} -f $(ELASTICSINK).Dockerfile .
	@docker push ${PREFIX}${ELASTICSINK}
.PHONY: pushelasticsink

pushgate:
	@docker build --no-cache --build-arg VERSION=${VERSION} --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t ${PREFIX}${GATE} -f $(GATE).Dockerfile .
	@docker push ${PREFIX}${GATE}
.PHONY: pushgate

pushgraphitesink:
	@docker build --no-cache --build-arg VERSION=${VERSION} --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t ${PREFIX}${GRAPHITESINK} -f $(GRAPHITESINK).Dockerfile .
	@docker push ${PREFIX}${GRAPHITESINK}
.PHONY: pushgraphitesink

pushmanagementapi:
	@docker build --no-cache --build-arg VERSION=${VERSION} --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t ${PREFIX}${MANAGEMENTAPI} -f $(MANAGEMENTAPI).Dockerfile .
	@docker push ${PREFIX}${MANAGEMENTAPI}
.PHONY: pushmanagementapi

pushsentrysink:
	@docker build --no-cache --build-arg VERSION=${VERSION} --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t ${PREFIX}${SENTRYSINK} -f $(SENTRYSINK).Dockerfile .
	@docker push ${PREFIX}${SENTRYSINK}
.PHONY: pushsentrysink

pushstreamapi:
	@docker build --no-cache --build-arg VERSION=${VERSION} --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t ${PREFIX}${STREAMAPI} -f $(STREAMAPI).Dockerfile .
	@docker push ${PREFIX}${STREAMAPI}
.PHONY: pushstreamapi

pushatreammanager:
	@docker build --no-cache --build-arg VERSION=${VERSION} --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t ${PREFIX}${STREAMMANAGER} -f $(STREAMMANAGER).Dockerfile .
	@docker push ${PREFIX}${STREAMMANAGER}
.PHONY: pushatreammanager

pushstreamsink:
	@docker build --no-cache --build-arg VERSION=${VERSION} --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t ${PREFIX}${STREAMSINK} -f $(STREAMSINK).Dockerfile .
	@docker push ${PREFIX}${STREAMSINK}
.PHONY: pushstreamsink

pushtimelineapi:
	@docker build --no-cache --build-arg VERSION=${VERSION} --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t ${PREFIX}${TIMELINEAPI} -f $(TIMELINEAPI).Dockerfile .
	@docker push ${PREFIX}${TIMELINEAPI}
.PHONY: pushtimelineapi

pushtimelinemanager:
	@docker build --no-cache --build-arg VERSION=${VERSION} --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t ${PREFIX}${TIMELINEMANAGER} -f $(TIMELINEMANAGER).Dockerfile .
	@docker push ${PREFIX}${TIMELINEMANAGER}
.PHONY: pushtimelinemanager

pushtimelinesink:
	@docker build --no-cache --build-arg VERSION=${VERSION} --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t ${PREFIX}${TIMELINESINK} -f $(TIMELINESINK).Dockerfile .
	@docker push ${PREFIX}${TIMELINESINK}
.PHONY: pushtimelinesink

pushtracingsink:
	@docker build --no-cache --build-arg VERSION=${VERSION} --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t ${PREFIX}${TRACINGSINK} -f $(TRACINGSINK).Dockerfile .
	@docker push ${PREFIX}${TRACINGSINK}
.PHONY: pushtracingsink
