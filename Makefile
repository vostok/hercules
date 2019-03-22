VERSION := $(shell mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
REPOSITORYNAME := tsypaev

SERVICES := hercules-elastic-sink \
	    hercules-gate \
	    hercules-sentry-sink \
	    hercules-stream-api \
	    hercules-stream-manager \
	    hercules-stream-sink \
	    hercules-timeline-api \
	    hercules-timeline-manager \
	    hercules-timeline-sink \
	    hercules-graphite-sink \
	    hercules-tracing-sink \
	    hercules-tracing-api 

.PHONY: push_all_images
push_all_images:
	for service in $(SERVICES) ; do \
		docker build --build-arg VERSION=$(VERSION) --build-arg SERVICENAME=$$service -t $(REPOSITORYNAME)/$$service:$(VERSION) -f Dockerfile . ; \
		docker push $(REPOSITORYNAME)/$$service:$(VERSION) ; \
	done


