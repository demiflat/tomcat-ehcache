# ![tomcat ehcache](tomcat-ehcache.jpg)                                                 

##### This project shows how to configure a simple servlet to run on Tomcat in Kubernetes with distributed session replication and Ehcache/Terracotta

###### Tested configurations:
 - [src/main/resources/ehcache.xml](src/main/resources/ehcache.xml)
  
This is the servlet Ehcache implementation basic configuration file. Not distributed (yet). [^1]

 - [src/main/resources/ehcache.distributed.xml](src/main/resources/ehcache.distributed.xml) 

This will be the servlet Ehcache implementation distributed configuration file. [^2]

#### Running make will list build targets

- DOCKER_REGISTRY 

Define environment variable $DOCKER_REGISTRY for the container registry used.

###### Local testing/Cluster testing tools

This runs a local Redis server for development testing

- [cli.newsession](cli.newsession)
  
This calls the server at $TARGET (default localhost) to create a new session

- [cli](cli)

After a session is created, this call the server at $TARGET (default localhost) to continue to use the session created previously.


###### Sample output:
```json
tbd
```
---
#### Kubernetes deployments
[k8s/k8s-role.yaml](k8s/k8s-role.yaml)
Kubernetes role, used to allow Tomcat to call Kubernetes API to get member list of other pods in the same namespace.

[k8s/k8s-deployment.yaml](k8s/k8s-deployment.yaml) 
Kubernetes deployment, set to create 3 replicas for testing.[^3]

[k8s/k8s-ingress.yaml](k8s/k8s-ingress.yaml) 
Kubernetes ingress, used to expose deployment externally.[^4]

[^1]: Ehcache [Config](https://www.ehcache.org/documentation/3.10/107.html)

[^2]: Terracotta [Reference](https://documentation.softwareag.com/terracotta/terracotta_10-11/webhelp/index.html)

[^3]: Kubernetes [Deployment](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/)

[^4]: Kubernetes [Ingress](https://kubernetes.io/docs/concepts/services-networking/ingress/)
