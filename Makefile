VERSION := $(shell mvn help:evaluate -Dexpression=project.version -q -DforceStdout)

SERVICES := hercules-gate \
	    hercules-management-api \
	    hercules-stream-manager \
	    hercules-stream-api \
	    hercules-stream-sink \
	    hercules-timeline-manager \
	    hercules-timeline-api \
	    hercules-timeline-sink \
	    hercules-tracing-api \
	    hercules-tracing-sink \
	    hercules-tracing-sink-clickhouse \
	    hercules-elastic-sink \
	    hercules-sentry-sink \
	    hercules-graphite-sink \
	    hercules-elastic-adapter


.PHONY: all build_images push_images
all: build_images push_images

build_images:
	for service in $(SERVICES) ; do \
		docker build --build-arg VERSION=$(VERSION) --build-arg SERVICENAME=$$service -t vstk/$$service:$(VERSION) -f Dockerfile . ; \
	done

push_images:
	for service in $(SERVICES) ; do \
		docker push vstk/$$service:$(VERSION) ; \
	done
