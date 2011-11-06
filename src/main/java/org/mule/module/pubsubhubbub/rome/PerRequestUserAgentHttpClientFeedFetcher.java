/**
 * Mule PubSubHubbub Connector
 *
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.pubsubhubbub.rome;

import com.sun.syndication.fetcher.impl.FeedFetcherCache;
import com.sun.syndication.fetcher.impl.HttpClientFeedFetcher;

/**
 * Supports per-request user agent setting, via a thread local.
 */
public class PerRequestUserAgentHttpClientFeedFetcher extends HttpClientFeedFetcher
{
    private final static ThreadLocal<String> REQUEST_USER_AGENT = new ThreadLocal<String>();

    public PerRequestUserAgentHttpClientFeedFetcher(final FeedFetcherCache cache)
    {
        super(cache);
    }

    public static void setRequestUserAgent(final String requestUserAgent)
    {
        REQUEST_USER_AGENT.set(requestUserAgent);
    }

    public static void removeRequestUserAgent()
    {
        REQUEST_USER_AGENT.remove();
    }

    @Override
    public synchronized String getUserAgent()
    {
        return REQUEST_USER_AGENT.get();
    }
}
