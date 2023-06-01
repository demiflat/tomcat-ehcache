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
TERRACOTTA=terracotta
TERRACOTTA_TAG:="$(REGISTRY)/$(TERRACOTTA):$(CONTAINER_VERSION)"
TERRACOTTA_PORT=9410

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
> podman build -f terracotta/Containerfile -t $(TERRACOTTA_TAG)

push: login docker
> podman push $(CONTAINER_TAG)

.PHONY: terracotta
terracotta:
> kubectl create deployment $(TERRACOTTA) --image=$(TERRACOTTA):latest --port TERRACOTTA_PORT --replicas=1
> kubectl create service clusterip $(TERRACOTTA) --tcp=TERRACOTTA_PORT:TERRACOTTA_PORT
> kubectl get all

terracotta-destroy:
> kubectl delete deployment $(TERRACOTTA) --ignore-not-found=true
> kubectl delete service $(TERRACOTTA) --ignore-not-found=true

deploy: push terracotta
> cat k8s/k8s-deployment.yaml | CONTAINER_TAG=$(CONTAINER_TAG) DEPLOYMENT=$(DEPLOYMENT) DEPLOYMENT_PORT=$(DEPLOYMENT_PORT) envsubst | kubectl apply -f -
> cat k8s/k8s-service.yaml | DEPLOYMENT=$(DEPLOYMENT) DEPLOYMENT_PORT=$(DEPLOYMENT_PORT) NAMESPACE=$(NAMESPACE) envsubst | kubectl apply -f -
> cat k8s/k8s-ingress.yaml | DEPLOYMENT=$(DEPLOYMENT) DEPLOYMENT_PORT=$(DEPLOYMENT_PORT) envsubst | kubectl apply -f -
> cat k8s/k8s-role.yaml | NAMESPACE=$(NAMESPACE) envsubst | kubectl apply -f -
> kubectl get all

destroy: terracotta-destroy
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
> kubectl exec -i -t dnsutils -- nslookup terracotta
