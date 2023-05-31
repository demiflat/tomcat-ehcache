package org.orbeon.session.tomcat;

import static java.util.Objects.nonNull;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.URI;
import org.ehcache.Cache;
import org.ehcache.PersistentCacheManager;
import org.ehcache.clustered.client.config.builders.ClusteredResourcePoolBuilder;
import org.ehcache.clustered.client.config.builders.ClusteredStoreConfigurationBuilder;
import org.ehcache.clustered.client.config.builders.ClusteringServiceConfigurationBuilder;
import org.ehcache.clustered.common.Consistency;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;
// import javax.cache.Cache;
// import javax.cache.CacheManager;
// import javax.cache.Caching;
// import javax.cache.spi.CachingProvider;

@WebServlet("/tomcat")
public class ClusteredServlet extends HttpServlet {
  public static final String ORBEON = "orbeon";
  String sessionKey = ClusteredServlet.class.getName();

  Cache<String, Serializable> cache;

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    String cacheName = config.getInitParameter("cacheName");
    if (isBlank(cacheName)) {
      // default:
      cacheName = ORBEON;
    }
    // tbd make this configurable
    try {
      final CacheManagerBuilder<PersistentCacheManager> clusteredCacheManagerBuilder =
          CacheManagerBuilder.newCacheManagerBuilder()
              .with(
                  ClusteringServiceConfigurationBuilder.cluster(
                          URI.create("terracotta://localhost:9410/orbeon"))
                      .autoCreateOnReconnect(
                          cfg -> cfg.resourcePool(ORBEON, 500, MemoryUnit.MB, ORBEON)));
      final PersistentCacheManager cacheManager = clusteredCacheManagerBuilder.build(true);

      CacheConfiguration<String, Serializable> cfg =
          CacheConfigurationBuilder.newCacheConfigurationBuilder(
                  String.class,
                  Serializable.class,
                  ResourcePoolsBuilder.newResourcePoolsBuilder()
                      .heap(20, MemoryUnit.MB)
                      .with(
                          ClusteredResourcePoolBuilder.clusteredDedicated(
                              ORBEON, 8, MemoryUnit.MB)))
              .add(ClusteredStoreConfigurationBuilder.withConsistency(Consistency.STRONG))
              .build();
      cache = cacheManager.createCache(cacheName, cfg);

      // jcache only via config (no terracotta it seems)
      // example xml cluster config barfs on parse:
      // Caused by: org.xml.sax.SAXParseException; systemId:
      // file:/var/home/dak/development/projects/orbeon/tomcat-ehcache/webapps/ROOT/WEB-INF/classes/ehcache.distributed.xml; lineNumber: 7; columnNumber: 21; cvc-complex-type.2.4.d: Invalid content was found starting with element 'tc:cluster'. No child element is expected at this point.
      //	at
      // java.xml/com.sun.org.apache.xerces.internal.util.ErrorHandlerWrapper.createSAXParseException(ErrorHandlerWrapper.java:204)
      //	at
      // java.xml/com.sun.org.apache.xerces.internal.util.ErrorHandlerWrapper.error(ErrorHandlerWrapper.java:135)
      //	at
      // java.xml/com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError(XMLErrorReporter.java:396)
      //	at
      // java.xml/com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError(XMLErrorReporter.java:327)
      //	at
      // java.xml/com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError(XMLErrorReporter.java:284)
      //      CachingProvider cachingProvider = Caching.getCachingProvider();
      //      CacheManager manager =
      //          cachingProvider.getCacheManager(
      //              getClass().getResource("/ehcache.distributed.xml").toURI(),
      //              getClass().getClassLoader());
      //      cache = manager.getCache(cacheName, String.class, Serializable.class);
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println("Unable to configure Ehcache JCache implementation");
    }
  }

  boolean isBlank(String string) {
    return string == null || string.trim().isEmpty();
  }

  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    HttpSession session = req.getSession();
    Integer count = Integer.class.cast(session.getAttribute(sessionKey));

    if (session.isNew()) {
      count = 0;
    } else {
      if (count == null) {
        // this shouldn't happen, make it obvious
        count = Integer.MIN_VALUE;
      }
    }
    session.setAttribute(sessionKey, ++count);

    Integer cachedEntry = 0;
    Serializable entry = cache.get(ORBEON);
    if (nonNull(entry)) {
      cachedEntry = Integer.class.cast(entry);
    }
    cache.put(ORBEON, ++cachedEntry);

    try (PrintWriter writer = resp.getWriter()) {
      writePayload(
          count, req.getLocalName(), req.getLocalAddr(), req.getLocalPort(), cachedEntry, writer);
    }
  }

  private void writePayload(
      Integer count,
      String localName,
      String localIp,
      Integer localPort,
      Integer cachedEntry,
      PrintWriter writer) {
    writer
        .append('{')
        .append("\"")
        .append("count")
        .append("\"")
        .append(":")
        .append(String.valueOf(count))
        .append(",")
        .append("\"")
        .append("localName")
        .append("\"")
        .append(":")
        .append("\"")
        .append(localName)
        .append("\"")
        .append(",")
        .append("\"")
        .append("localName")
        .append("\"")
        .append(":")
        .append("\"")
        .append(localIp)
        .append("\"")
        .append(",")
        .append("\"")
        .append("localPort")
        .append("\"")
        .append(":")
        .append("\"")
        .append(String.valueOf(localPort))
        .append("\"")
        .append(",")
        .append("\"")
        .append("cachedEntry")
        .append("\"")
        .append(":")
        .append("\"")
        .append(String.valueOf(cachedEntry))
        .append("\"")
        .append('}');
  }
}
