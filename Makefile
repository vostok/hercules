VERSION := $(shell mvn help:evaluate -Dexpression=project.version -q -DforceStdout)

SERVICES := hercules-elastic-adapter \
        hercules-elastic-sink \
        hercules-gate \
        hercules-graphite-adapter \
        hercules-graphite-sink \
        hercules-management-api \
        hercules-sentry-sink \
        hercules-stream-api \
        hercules-stream-manager \
        hercules-stream-sink \
        hercules-timeline-api \
        hercules-timeline-manager \
        hercules-timeline-sink \
        hercules-tracing-api \
        hercules-tracing-sink \
        hercules-tracing-sink-clickhouse \
        hercules-init \
        hercules-routing \
        hercules-opentelemetry-adapter


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
