VERSION := $(shell mvn org.apache.maven.plugins:maven-help-plugin:3.1.0:evaluate -Dexpression=project.version -q -DforceStdout)
PREFIX := hercules

ELASTICSINK := hercules-elastic-sink 
GATE := hercules-gate
GRAPHITESINK := hercules-graphite-sink
MANAGEMENTAPI := hercules-management-api
SENTRYSINK := hercules-sentry-sink
STREAMAPI := hercules-stream-api
STREAMMANAGER := hercules-stream-manager
STREAMSINK := hercules-stream-sink
TIMELINEAPI := hercules-timeline-api
TIMELINEMANAGER := hercules-timeline-manager
TIMELINESINK := hercules-timeline-sink
TRACINGSINK := hercules-tracing-sink

SERVICES=$(ELASTICSINK) $(GATE) $(GRAPHITESINK) $(MANAGEMENTAPI) $(SENTRYSINK) $(STREAMAPI) $(STREAMMANAGER) $(STREAMSINK) $(TIMELINEAPI) $(TIMELINEMANAGER) $(TIMELINESINK) $(TRACINGSINK) 

.PHONY: push_all_images
push_all_images:
	for service in $(SERVICES) ; do \
		docker build --build-arg VERSION=${VERSION} --build-arg SERVICENAME=$$service -t ${PREFIX}/$$service:${VERSION} -f Dockerfile . ; \
		docker push ${PREFIX}/$$service:${VERSION} ; \
	done

