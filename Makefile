SHELL := bash
.ONESHELL:
.SHELLFLAGS := -eu -o pipefail -c
.DELETE_ON_ERROR:
MAKEFLAGS += --warn-undefined-variables
MAKEFLAGS += --no-builtin-rules
ifeq ($(origin .RECIPEPREFIX), undefined)
  $(error This Make does not support .RECIPEPREFIX. Please use GNU Make 4.0 or later)
endif
.RECIPEPREFIX = >

REGISTRY:=$(DOCKER_REGISTRY)
CONTAINER_NAME=tomcat-ehcache
CONTAINER_VERSION=0
CONTAINER_TAG:="$(REGISTRY)/$(CONTAINER_NAME):$(CONTAINER_VERSION)"
DEPLOYMENT=tomcat-ehcache
DEPLOYMENT_PORT=8080
NAMESPACE=default

info:
> @cat .info

all: login deploy kube-info kube-info-all

clean:
> rm -rf build

build:
> ./gradlew war

login:
> podman login $(REGISTRY)

docker: build
> podman build -f Containerfile -t $(CONTAINER_TAG)

push: login docker
> podman push $(CONTAINER_TAG)

.PHONY: redis
redis:
> kubectl create deployment $(REDIS) --image=$(REDIS):latest --port 6479 --replicas=1
> kubectl create service clusterip redis --tcp=6379:6379
> kubectl get all

redis-destroy:
> kubectl delete deployment $(REDIS) --ignore-not-found=true
> kubectl delete service $(REDIS) --ignore-not-found=true

deploy: push redis
> cat k8s/k8s-deployment.yaml | CONTAINER_TAG=$(CONTAINER_TAG) DEPLOYMENT=$(DEPLOYMENT) DEPLOYMENT_PORT=$(DEPLOYMENT_PORT) envsubst | kubectl apply -f -
> cat k8s/k8s-service.yaml | DEPLOYMENT=$(DEPLOYMENT) DEPLOYMENT_PORT=$(DEPLOYMENT_PORT) NAMESPACE=$(NAMESPACE) envsubst | kubectl apply -f -
> cat k8s/k8s-ingress.yaml | DEPLOYMENT=$(DEPLOYMENT) DEPLOYMENT_PORT=$(DEPLOYMENT_PORT) envsubst | kubectl apply -f -
> cat k8s/k8s-role.yaml | NAMESPACE=$(NAMESPACE) envsubst | kubectl apply -f -
> kubectl get all

destroy: redis-destroy
> kubectl delete deployment $(DEPLOYMENT) --ignore-not-found=true
> kubectl delete service $(DEPLOYMENT) --ignore-not-found=true
> kubectl delete ingress $(DEPLOYMENT) --ignore-not-found=true
> kubectl get all

watch:
> watch -n 2 $(MAKE) kube-info

kube-info:
> kubectl get all

kube-info-all:
> kubectl get -A all

kube-dns-utils:
> kubectl apply -f https://k8s.io/examples/admin/dns/dnsutils.yaml

kube-dns-redis:
> kubectl exec -i -t dnsutils -- nslookup redis
