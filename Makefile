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

pushelasticsink:
	@docker build --no-cache --build-arg VERSION=${VERSION} --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t ${PREFIX}${ELASTICSINK} -f $(ELASTICSINK).Dockerfile .
	@docker push ${PREFIX}${ELASTICSINK}
.PHONY: pushelasticsink

pushgate:
	@docker build --no-cache --build-arg VERSION=${VERSION} --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t ${PREFIX}${GATE} -f $(GATEBUILDERFILE) .
	@docker push ${PREFIX}${GATE}
.PHONY: pushgate

pushgraphitesink:
	@docker build --no-cache --build-arg VERSION=${VERSION} --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t ${PREFIX}${GRAPHITESINK} -f $(GRAPHITESINKBUILDERFILE) .
	@docker push ${PREFIX}${GRAPHITESINK}
.PHONY: pushgraphitesink

pushmanagementapi:
	@docker build --no-cache --build-arg VERSION=${VERSION} --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t ${PREFIX}${MANAGEMENTAPI} -f $(MANAGEMENTAPIBUILDERFILE) .
	@docker push ${PREFIX}${MANAGEMENTAPI}
.PHONY: pushmanagementapi

pushsentrysink:
	@docker build --no-cache --build-arg VERSION=${VERSION} --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t ${PREFIX}${SENTRYSINK} -f $(SENTRYSINKBUILDERFILE) .
	@docker push ${PREFIX}${SENTRYSINK}
.PHONY: pushsentrysink

pushstreamapi:
	@docker build --no-cache --build-arg VERSION=${VERSION} --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t ${PREFIX}${STREAMAPI} -f $(STREAMAPIBUILDERFILE) .
	@docker push ${PREFIX}${STREAMAPI}
.PHONY: pushstreamapi

pushatreammanager:
	@docker build --no-cache --build-arg VERSION=${VERSION} --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t ${PREFIX}${STREAMMANAGER} -f $(STREAMMANAGERBUILDERFILE) .
	@docker push ${PREFIX}${STREAMMANAGER}
.PHONY: pushatreammanager

pushstreamsink:
	@docker build --no-cache --build-arg VERSION=${VERSION} --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t ${PREFIX}${STREAMSINK} -f $(STREAMSINKBUILDERFILE) .
	@docker push ${PREFIX}${STREAMSINK}
.PHONY: pushstreamsink

pushtimelineapi:
	@docker build --no-cache --build-arg VERSION=${VERSION} --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t ${PREFIX}${TIMELINEAPI} -f $(TIMELINEAPIBUILDERFILE) .
	@docker push ${PREFIX}${TIMELINEAPI}
.PHONY: pushtimelineapi

pushtimelinemanager:
	@docker build --no-cache --build-arg VERSION=${VERSION} --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t ${PREFIX}${TIMELINEMANAGER} -f $(TIMELINEMANAGERBUILDERFILE) .
	@docker push ${PREFIX}${TIMELINEMANAGER}
.PHONY: pushtimelinemanager

pushtracingsink:
	@docker build --no-cache --build-arg VERSION=${VERSION} --build-arg WORKDIR=${WORKDIR} --build-arg JAVAOPTS=${JAVAOPTS} --build-arg SETTINGS=${SETTINGS} -t ${PREFIX}${TRACINGSINK} -f $(TRACINGSINKBUILDERFILE) .
	@docker push ${PREFIX}${TRACINGSINK}
.PHONY: pushtracingsink

pushallimages: pushelasticsink pushgate pushgraphitesink pushmanagementapi pushsentrysink pushstreamapi pushatreammanager pushstreamsink pushtimelineapi pushtimelinemanager pushtracingsink
.PHONY: pushallimages


