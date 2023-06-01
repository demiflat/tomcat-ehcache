plugins {
    java
    war
}
group = "org.orbeon.session"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
        mavenCentral()
        maven {
            url = uri("https://repo.terracotta.org/maven2")
        }
}

var tomcatVersion = "10.1.8"
dependencies {
    compileOnly("org.apache.tomcat:tomcat-catalina:${tomcatVersion}")
    compileOnly("org.apache.tomcat:tomcat-tribes:${tomcatVersion}")
    implementation("org.terracotta.internal:client-runtime:5.9.5")
    implementation("org.ehcache:ehcache:3.10.8")
    implementation("org.ehcache:ehcache-clustered:3.10.8")
}