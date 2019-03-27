VERSION := $(shell mvn help:evaluate -Dexpression=project.version -q -DforceStdout)

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

.PHONY: services_images
services_images:
	for service in $(SERVICES) ; do \
		docker build --build-arg VERSION=$(VERSION) --build-arg SERVICENAME=$$service -t vstk/$$service:$(VERSION) -f Dockerfile . ; \
		docker push vstk/$$service:$(VERSION) ; \
	done

