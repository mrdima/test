IMAGE_NAMESPACE=kpnappfactory
IMAGE_NAME=$(notdir $(subst docker-,,$(CURDIR)))
IMAGE_ID=$(shell docker images -q $(IMAGE_NAMESPACE)/$(IMAGE_NAME):$(IMAGE_VERSION) | head -1)
IMAGE_VERSION=latest
TAG_NAME=$(IMAGE_NAME)
TAG_VERSION=$(shell git rev-parse --short HEAD)
VOLUME_WORKDIR=$(CURDIR)

build:
	docker build --rm -t kpnappfactory/itvTest:$IMAGE_VERSION .

clean:
	docker rmi -f $(IMAGE_NAMESPACE)/$(IMAGE_NAME):$(IMAGE_VERSION)

integrationtest:
	docker run --rm -i -e "CI_REPORTS=/usr/src/app/spec/$(IMAGE_NAME)/reports/" -v /var/run/docker.sock:/var/run/docker.sock -v $(VOLUME_WORKDIR):/usr/src/app/spec/$(IMAGE_NAME) kpnappfactory/serverspec test\["$(IMAGE_NAME)","$(IMAGE_ID)"\]

tag:
	docker tag $(IMAGE_NAMESPACE)/$(IMAGE_NAME):$(IMAGE_VERSION) $(TAG_NAME):$(TAG_VERSION)

publish: tag
	docker push $(TAG_NAME):$(TAG_VERSION)

.PHONY: build clean test tag publish