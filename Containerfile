FROM eclipse-temurin:11-jdk-jammy

ENV JAVA_OPTS="-Dcatalina.base=. -Djava.security.egd=file:/dev/urandom"
#ENV JAVA_OPTS="${JAVA_OPTS} -Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager -Djava.util.logging.config.file=conf/logging.properties"
ENV KUBERNETES_NAMESPACE="default"
WORKDIR /deployments
COPY conf/ /deployments/conf/
ADD tomcat.deploy /deployments/tomcat
ADD tomcat.jar /deployments
ADD tomcat.version /deployments
ADD build/libs/tomcat-ehcache-0.0.1-SNAPSHOT.war /deployments/webapps/ehcache.war

EXPOSE 8080
ENTRYPOINT ["bash", "tomcat"]
