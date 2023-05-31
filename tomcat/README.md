To create the cloud deployable version of Tomcat following the instructions outlined in Tomcat [1]

The only modification made is the addition of the Ehcache jars.

The version of Tomcat used was 10.1.9.

To apply the diff, checkout Tomcat under the 10.1.9 tag and apply patch.
```
git apply ehcache.patch
```

For convienience, the tomcat.jar is also included here.
