package io.opentracing.contrib.tag;

import io.opentracing.tag.StringTag;

/**
 * Additional suggested standard tag names that conform to
 * the OpenTracing naming patterns.
 *
 * @author ashley.mercer@skylightipv.com
 */
public final class ExtraTags {

    private ExtraTags(){}

    /**
     * VERSION records the software version of the current application
     */
    public static final StringTag VERSION = new StringTag("version");

    /**
     * DB_DRIVER records the name and / or version number of the client-side
     * database driver
     */
    public static final StringTag DB_DRIVER = new StringTag("db.driver");

    /**
     * DB_VERSION captures the database server-side version String
     */
    public static final StringTag DB_VERSION = new StringTag("db.version");

    /**
     * HTTP_CLIENT_IP records the IP address of the requester, could be captured
     * e.g. from the IP packet's 'Source Address' or the X-Forwarded-For header
     */
    public static final StringTag HTTP_CLIENT_IP = new StringTag("http.client_ip");

    /**
     * HTTP_USER_AGENT records the HTTP client's User-Agent header
     */
    public static final StringTag HTTP_USER_AGENT = new StringTag("http.user_agent");

    /**
     * HTTP_CONTENT_LENGTH records the number of bytes returned to the client
     * by the server over the course of the request
     */
    public static final StringTag HTTP_CONTENT_LENGTH = new StringTag("http.content_length");
}
