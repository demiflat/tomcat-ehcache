package org.orbeon.session.tomcat;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;

import static java.util.Objects.nonNull;

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
            cacheName = "orbeon";
        }
        try {
            CachingProvider cachingProvider = Caching.getCachingProvider();
            CacheManager manager = cachingProvider.getCacheManager(
                    getClass().getResource("/ehcache.xml").toURI(),
                    getClass().getClassLoader());
            cache = manager.getCache(cacheName, String.class, Serializable.class);
            int i = 1;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(
                    "Unable to configure Ehcache JCache implementation");
        }
    }

    boolean isBlank(String string) {
        return string == null || string.trim().isEmpty();
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
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

        try(PrintWriter writer = resp.getWriter()) {
            writePayload(count, req.getLocalName(), req.getLocalAddr(), req.getLocalPort(), cachedEntry, writer);
        }
    }

    private void writePayload(Integer count, String localName, String localIp, Integer localPort, Integer cachedEntry, PrintWriter writer) {
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
