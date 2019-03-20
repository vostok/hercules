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
TRACINGSINK := tracingsink

.PHONY: pushallimages
pushallimages: pushelasticsink pushgate pushgraphitesink pushmanagementapi pushsentrysink pushstreamapi pushatreammanager pushstreamsink pushtimelineapi pushtimelinemanager pushtracingsink

pushelasticsink:
	@docker build --no-cache --build-arg VERSION=${VERSION} --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t ${PREFIX}${ELASTICSINK} -f $(ELASTICSINK).Dockerfile .
	@docker push ${PREFIX}${ELASTICSINK}
.PHONY: pushelasticsink

pushgate:
	@docker build --no-cache --build-arg VERSION=${VERSION} --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t ${PREFIX}${GATE} -f $(GATE) .
	@docker push ${PREFIX}${GATE}
.PHONY: pushgate

pushgraphitesink:
	@docker build --no-cache --build-arg VERSION=${VERSION} --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t ${PREFIX}${GRAPHITESINK} -f $(GRAPHITESINK) .
	@docker push ${PREFIX}${GRAPHITESINK}
.PHONY: pushgraphitesink

pushmanagementapi:
	@docker build --no-cache --build-arg VERSION=${VERSION} --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t ${PREFIX}${MANAGEMENTAPI} -f $(MANAGEMENTAPI) .
	@docker push ${PREFIX}${MANAGEMENTAPI}
.PHONY: pushmanagementapi

pushsentrysink:
	@docker build --no-cache --build-arg VERSION=${VERSION} --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t ${PREFIX}${SENTRYSINK} -f $(SENTRYSINK) .
	@docker push ${PREFIX}${SENTRYSINK}
.PHONY: pushsentrysink

pushstreamapi:
	@docker build --no-cache --build-arg VERSION=${VERSION} --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t ${PREFIX}${STREAMAPI} -f $(STREAMAPI) .
	@docker push ${PREFIX}${STREAMAPI}
.PHONY: pushstreamapi

pushatreammanager:
	@docker build --no-cache --build-arg VERSION=${VERSION} --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t ${PREFIX}${STREAMMANAGER} -f $(STREAMMANAGER) .
	@docker push ${PREFIX}${STREAMMANAGER}
.PHONY: pushatreammanager

pushstreamsink:
	@docker build --no-cache --build-arg VERSION=${VERSION} --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t ${PREFIX}${STREAMSINK} -f $(STREAMSINK) .
	@docker push ${PREFIX}${STREAMSINK}
.PHONY: pushstreamsink

pushtimelineapi:
	@docker build --no-cache --build-arg VERSION=${VERSION} --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t ${PREFIX}${TIMELINEAPI} -f $(TIMELINEAPI) .
	@docker push ${PREFIX}${TIMELINEAPI}
.PHONY: pushtimelineapi

pushtimelinemanager:
	@docker build --no-cache --build-arg VERSION=${VERSION} --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t ${PREFIX}${TIMELINEMANAGER} -f $(TIMELINEMANAGER) .
	@docker push ${PREFIX}${TIMELINEMANAGER}
.PHONY: pushtimelinemanager

pushtracingsink:
	@docker build --no-cache --build-arg VERSION=${VERSION} --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t ${PREFIX}${TRACINGSINK} -f $(TRACINGSINK) .
	@docker push ${PREFIX}${TRACINGSINK}
.PHONY: pushtracingsink
