/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.pubsubhubbub.handler;

import java.net.URI;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.mule.api.retry.RetryCallback;
import org.mule.api.retry.RetryContext;
import org.mule.module.pubsubhubbub.Constants;
import org.mule.module.pubsubhubbub.HubResource;

import com.sun.syndication.fetcher.FeedFetcher;
import com.sun.syndication.fetcher.FetcherEvent;
import com.sun.syndication.fetcher.FetcherListener;

public class PublisherHandler extends AbstractHubActionHandler implements FetcherListener
{
    private FeedFetcher feedFetcher;

    public static class ContentFetch implements RetryCallback
    {
        private final FeedFetcher feedFetcher;
        private final URI hubUrl;

        protected ContentFetch(final FeedFetcher feedFetcher, final URI hubUrl)
        {
            Validate.notNull(feedFetcher, "feedFetcher can't be null");
            Validate.notNull(hubUrl, "hubUrl can't be null");

            this.feedFetcher = feedFetcher;
            this.hubUrl = hubUrl;
        }

        public String getWorkDescription()
        {
            return "Fetching content of: " + hubUrl;
        }

        public void doWork(final RetryContext context) throws Exception
        {
            // we ignore the result here and rely on the feed fetcher event EVENT_TYPE_FEED_RETRIEVED to fire if the
            // feed has been actually retrieved from the web instead of being just read from cache
            feedFetcher.retrieveFeed(hubUrl.toURL());
        }
    }

    @Override
    public Response handle(final MultivaluedMap<String, String> formParams)
    {
        final List<URI> hubUrls = HubResource.getMandatoryUrlParameters(Constants.HUB_URL_PARAM, formParams);
        for (final URI hubUrl : hubUrls)
        {
            try
            {
                getRetryPolicyTemplate().execute(new ContentFetch(getFeedFetcher(), hubUrl),
                    getMuleContext().getWorkManager());
            }
            catch (final Exception e)
            {
                final String errorMessage = "Failed to schedule content fetch for: " + hubUrl;
                getLogger().error(errorMessage, e);
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity(errorMessage).build();
            }
        }

        return Response.noContent().build();
    }

    public void fetcherEvent(final FetcherEvent event)
    {
        if (StringUtils.equals(event.getEventType(), FetcherEvent.EVENT_TYPE_FEED_UNCHANGED))
        {
            getLogger().info("Content distribution skipped for unchanged feed: " + event.getUrlString());
            return;
        }

        if (StringUtils.equals(event.getEventType(), FetcherEvent.EVENT_TYPE_FEED_RETRIEVED))
        {
            // FIXME implement!
            getLogger().warn("!!! should distribute: " + ToStringBuilder.reflectionToString(event));
        }
    }

    public void setFeedFetcher(final FeedFetcher feedFetcher)
    {
        this.feedFetcher = feedFetcher;
        feedFetcher.addFetcherEventListener(this);
    }

    public FeedFetcher getFeedFetcher()
    {
        return feedFetcher;
    }
}
