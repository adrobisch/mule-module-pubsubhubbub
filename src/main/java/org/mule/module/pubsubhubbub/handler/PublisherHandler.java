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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.resource.spi.work.Work;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.retry.RetryCallback;
import org.mule.api.retry.RetryContext;
import org.mule.module.client.MuleClient;
import org.mule.module.pubsubhubbub.Constants;
import org.mule.module.pubsubhubbub.HubResource;
import org.mule.module.pubsubhubbub.data.TopicSubscription;
import org.mule.transport.http.HttpConnector;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.fetcher.FeedFetcher;
import com.sun.syndication.fetcher.FetcherEvent;
import com.sun.syndication.fetcher.FetcherListener;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.WireFeedOutput;

public class PublisherHandler extends AbstractHubActionHandler implements FetcherListener
{
    private FeedFetcher feedFetcher;

    public static class ContentFetchWork implements Work
    {
        private static final Log LOG = LogFactory.getLog(ContentFetchWork.class);
        private final FeedFetcher feedFetcher;
        private final URI hubUrl;

        protected ContentFetchWork(final FeedFetcher feedFetcher, final URI hubUrl)
        {
            Validate.notNull(feedFetcher, "feedFetcher can't be null");
            Validate.notNull(hubUrl, "hubUrl can't be null");

            this.feedFetcher = feedFetcher;
            this.hubUrl = hubUrl;
        }

        public void run()
        {
            // we ignore the result here and rely on the feed fetcher event EVENT_TYPE_FEED_RETRIEVED to fire if the
            // feed has been actually retrieved from the web instead of being just read from cache
            try
            {
                feedFetcher.retrieveFeed(hubUrl.toURL());
            }
            catch (final Exception e)
            {
                LOG.error("Failed to fetch content from: " + hubUrl, e);
            }
        }

        public void release()
        {
            // NOOP
        }
    }

    public static class DistributeContentRetryCallback implements RetryCallback
    {
        private static final Log LOG = LogFactory.getLog(DistributeContentRetryCallback.class);
        private final MuleContext muleContext;
        private final String contentType;
        private final URI callbackUrl;

        protected final String distributedContent;

        protected DistributeContentRetryCallback(final MuleContext muleContext,
                                                 final String contentType,
                                                 final String distributedContent,
                                                 final URI callbackUrl)
        {
            this.muleContext = muleContext;
            this.contentType = contentType;
            this.distributedContent = distributedContent;
            this.callbackUrl = callbackUrl;
        }

        public String getWorkDescription()
        {
            return "Distributing content to " + callbackUrl;
        }

        public void doWork(final RetryContext context) throws Exception
        {
            final Map<String, String> headers = new HashMap<String, String>();
            addHeaders(headers);

            final MuleMessage response = new MuleClient(muleContext).send(callbackUrl.toString(),
                distributedContent, headers, (int) Constants.SUBSCRIBER_TIMEOUT_MILLIS);

            if (response == null)
            {
                throw new TimeoutException("Failed to send content to: " + callbackUrl);
            }

            final String getResponseStatusCode = response.getInboundProperty(
                HttpConnector.HTTP_STATUS_PROPERTY, "nil");

            if (!StringUtils.startsWith(getResponseStatusCode, "2"))
            {
                throw new IllegalArgumentException("Received status " + getResponseStatusCode + " from: "
                                                   + callbackUrl);
            }

            LOG.info("Successfully distributed content to: " + callbackUrl);
        }

        protected void addHeaders(final Map<String, String> headers)
        {
            headers.put(HttpHeaders.CONTENT_TYPE, contentType);
        }
    }

    public static final class DistributeAuthenticatedContentRetryCallback extends
        DistributeContentRetryCallback
    {
        private final String signature;

        protected DistributeAuthenticatedContentRetryCallback(final MuleContext muleContext,
                                                              final String contentType,
                                                              final String distributedContent,
                                                              final URI callbackUrl,
                                                              final byte[] secret) throws Exception
        {
            super(muleContext, contentType, distributedContent, callbackUrl);
            signature = computeSignature(secret);
        }

        @Override
        protected void addHeaders(final Map<String, String> headers)
        {
            super.addHeaders(headers);
            headers.put(Constants.HUB_SIGNATURE_HEADER, "sha1=" + signature);
        }

        private String computeSignature(final byte[] secret)
            throws GeneralSecurityException, UnsupportedEncodingException
        {
            final SecretKeySpec secretKey = new SecretKeySpec(secret, "HmacSHA1");
            final Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(secretKey);
            // TODO use distributed content encoding
            final byte[] rawHmac = mac.doFinal(distributedContent.getBytes());
            return new String(Base64.encodeBase64(rawHmac));
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

    @Override
    public Response handle(final MultivaluedMap<String, String> formParams)
    {
        final List<URI> hubUrls = HubResource.getMandatoryUrlParameters(Constants.HUB_URL_PARAM, formParams);
        for (final URI hubUrl : hubUrls)
        {
            try
            {
                getMuleContext().getWorkManager()
                    .scheduleWork(new ContentFetchWork(getFeedFetcher(), hubUrl));
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

        if (!StringUtils.equals(event.getEventType(), FetcherEvent.EVENT_TYPE_FEED_RETRIEVED))
        {
            return;
        }

        try
        {
            final URI topicUrl = new URI(event.getUrlString());
            processRetrievedFeed(topicUrl, event.getFeed());
        }
        catch (final Exception e)
        {
            getLogger().error("Failed to process feed retrieved from: " + event.getUrlString(), e);
        }
    }

    private void processRetrievedFeed(final URI topicUrl, final SyndFeed feed) throws Exception
    {
        final List<SyndEntry> newEntries = findNewFeedEntries(topicUrl, feed);

        if (newEntries.isEmpty())
        {
            getLogger().info(
                "Publisher has requested content distribution but no new feed entries have been found for: "
                                + topicUrl);
            return;
        }

        final Set<TopicSubscription> topicSubscriptions = getDataStore().getTopicSubscriptions(topicUrl);
        if (!topicSubscriptions.isEmpty())
        {
            final String distributedContent = createDistributedContent(feed, newEntries);
            final String contentType = getDistributedContentType(feed);
            distributeContent(contentType, distributedContent, topicSubscriptions);
        }
        else
        {
            getLogger().info("No subscriber found for content distribution of: " + topicUrl);
        }

        storeNewFeedEntries(topicUrl, newEntries);
    }

    private String getDistributedContentType(final SyndFeed feed)
    {
        return StringUtils.containsIgnoreCase(feed.getFeedType(), "rss")
                                                                        ? Constants.RSS_CONTENT_TYPE
                                                                        : Constants.ATOM_CONTENT_TYPE;
    }

    private String createDistributedContent(final SyndFeed templateFeed, final List<SyndEntry> newEntries)
        throws CloneNotSupportedException, FeedException
    {
        final SyndFeed cloned = (SyndFeed) templateFeed.clone();
        cloned.setEntries(newEntries);
        return new WireFeedOutput().outputString(cloned.createWireFeed());
    }

    private void storeNewFeedEntries(final URI topicUrl, final List<SyndEntry> newEntries)
    {
        for (final SyndEntry entry : newEntries)
        {
            getDataStore().storeTopicFeedId(topicUrl, getFeedEntryId(entry));
        }
    }

    private List<SyndEntry> findNewFeedEntries(final URI topicUrl, final SyndFeed feed)
    {
        final Set<String> knownFeedIds = getDataStore().getTopicFeedIds(topicUrl);
        final List<SyndEntry> newEntries = new ArrayList<SyndEntry>();
        @SuppressWarnings("unchecked")
        final List<SyndEntry> retrievedEntries = feed.getEntries();
        for (final SyndEntry entry : retrievedEntries)
        {
            if (!knownFeedIds.contains(getFeedEntryId(entry)))
            {
                newEntries.add(entry);
            }
        }
        return newEntries;
    }

    private String getFeedEntryId(final SyndEntry entry)
    {
        // we use the URI as feed entry ID as it is present for both RSS and ATOM entries
        return entry.getUri();
    }

    private void distributeContent(final String contentType,
                                   final String distributedContent,
                                   final Set<TopicSubscription> topicSubscriptions) throws Exception
    {
        for (final TopicSubscription topicSubscription : topicSubscriptions)
        {
            if (topicSubscription.getSecret() != null)
            {
                getRetryPolicyTemplate().execute(
                    new DistributeAuthenticatedContentRetryCallback(getMuleContext(), contentType,
                        distributedContent, topicSubscription.getCallbackUrl(), topicSubscription.getSecret()),
                    getMuleContext().getWorkManager());
            }
            else
            {
                getRetryPolicyTemplate().execute(
                    new DistributeContentRetryCallback(getMuleContext(), contentType, distributedContent,
                        topicSubscription.getCallbackUrl()), getMuleContext().getWorkManager());
            }
        }
    }
}
