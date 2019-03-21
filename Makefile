VERSION := $(shell mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
REPOSITORYNAME := tsypaev

ELASTICSINK := hercules-elastic-sink 
GATE := hercules-gate
MANAGEMENTAPI := hercules-management-api
SENTRYSINK := hercules-sentry-sink
STREAMAPI := hercules-stream-api
STREAMMANAGER := hercules-stream-manager
STREAMSINK := hercules-stream-sink
TIMELINEAPI := hercules-timeline-api
TIMELINEMANAGER := hercules-timeline-manager
TIMELINESINK := hercules-timeline-sink

SERVICES=$(ELASTICSINK) $(GATE) $(GRAPHITESINK) $(MANAGEMENTAPI) $(SENTRYSINK) $(STREAMAPI) $(STREAMMANAGER) $(STREAMSINK) $(TIMELINEAPI) $(TIMELINEMANAGER) $(TIMELINESINK) $(TRACINGSINK) 

.PHONY: push_all_images
push_all_images:
	for service in $(SERVICES) ; do \
		docker build --build-arg VERSION=$(VERSION) --build-arg SERVICENAME=$$service -t $(REPOSITORYNAME)/$$service:$(VERSION) -f Dockerfile . ; \
		docker push $(REPOSITORYNAME)/$$service:$(VERSION) ; \
	done


