MAVENIMAGENAME := maven
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

pushallimages: pushelasticsink pushgate pushgraphitesink pushmanagementapi pushsentrysink pushstreamapi pushatreammanager pushstreamsink pushtimelineapi pushtimelinemanager pushtimelinesink pushtracingsink
.PHONY: pushallimages

pushelasticsink:
	@docker build --build-arg VERSION=${VERSION} -t ${PREFIX}${ELASTICSINK} -f $(ELASTICSINK).Dockerfile .
	@docker push ${PREFIX}${ELASTICSINK}
.PHONY: pushelasticsink

pushgate:
	@docker build --build-arg VERSION=${VERSION} -t ${PREFIX}${GATE} -f $(GATE).Dockerfile .
	@docker push ${PREFIX}${GATE}
.PHONY: pushgate

pushgraphitesink:
	@docker build --build-arg VERSION=${VERSION} -t ${PREFIX}${GRAPHITESINK} -f $(GRAPHITESINK).Dockerfile .
	@docker push ${PREFIX}${GRAPHITESINK}
.PHONY: pushgraphitesink

pushmanagementapi:
	@docker build --build-arg VERSION=${VERSION} -t ${PREFIX}${MANAGEMENTAPI} -f $(MANAGEMENTAPI).Dockerfile .
	@docker push ${PREFIX}${MANAGEMENTAPI}
.PHONY: pushmanagementapi

pushsentrysink:
	@docker build --build-arg VERSION=${VERSION} -t ${PREFIX}${SENTRYSINK} -f $(SENTRYSINK).Dockerfile .
	@docker push ${PREFIX}${SENTRYSINK}
.PHONY: pushsentrysink

pushstreamapi:
	@docker build --build-arg VERSION=${VERSION} -t ${PREFIX}${STREAMAPI} -f $(STREAMAPI).Dockerfile .
	@docker push ${PREFIX}${STREAMAPI}
.PHONY: pushstreamapi

pushatreammanager:
	@docker build --build-arg VERSION=${VERSION} -t ${PREFIX}${STREAMMANAGER} -f $(STREAMMANAGER).Dockerfile .
	@docker push ${PREFIX}${STREAMMANAGER}
.PHONY: pushatreammanager

pushstreamsink:
	@docker build --build-arg VERSION=${VERSION} -t ${PREFIX}${STREAMSINK} -f $(STREAMSINK).Dockerfile .
	@docker push ${PREFIX}${STREAMSINK}
.PHONY: pushstreamsink

pushtimelineapi:
	@docker build --build-arg VERSION=${VERSION} -t ${PREFIX}${TIMELINEAPI} -f $(TIMELINEAPI).Dockerfile .
	@docker push ${PREFIX}${TIMELINEAPI}
.PHONY: pushtimelineapi

pushtimelinemanager:
	@docker build --build-arg VERSION=${VERSION} -t ${PREFIX}${TIMELINEMANAGER} -f $(TIMELINEMANAGER).Dockerfile .
	@docker push ${PREFIX}${TIMELINEMANAGER}
.PHONY: pushtimelinemanager

pushtimelinesink:
	@docker build --build-arg VERSION=${VERSION} -t ${PREFIX}${TIMELINESINK} -f $(TIMELINESINK).Dockerfile .
	@docker push ${PREFIX}${TIMELINESINK}
.PHONY: pushtimelinesink

pushtracingsink:
	@docker build --build-arg VERSION=${VERSION} -t ${PREFIX}${TRACINGSINK} -f $(TRACINGSINK).Dockerfile .
	@docker push ${PREFIX}${TRACINGSINK}
.PHONY: pushtracingsink
