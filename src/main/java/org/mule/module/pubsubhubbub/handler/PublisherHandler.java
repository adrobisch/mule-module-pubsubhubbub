/**
 * Mule PubSubHubbub Connector
 *
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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.retry.RetryCallback;
import org.mule.api.retry.RetryContext;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.module.client.MuleClient;
import org.mule.module.pubsubhubbub.Constants;
import org.mule.module.pubsubhubbub.HubResponse;
import org.mule.module.pubsubhubbub.HubUtils;
import org.mule.module.pubsubhubbub.data.DataStore;
import org.mule.module.pubsubhubbub.data.TopicSubscription;
import org.mule.module.pubsubhubbub.rome.PerRequestUserAgentHttpClientFeedFetcher;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.fetcher.FeedFetcher;
import com.sun.syndication.fetcher.FetcherEvent;
import com.sun.syndication.fetcher.FetcherListener;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.WireFeedOutput;

public class PublisherHandler extends AbstractHubActionHandler implements FetcherListener
{
    public static class ContentFetchWork implements Work
    {
        private static final Log LOG = LogFactory.getLog(ContentFetchWork.class);

        private final DataStore dataStore;
        private final FeedFetcher feedFetcher;
        private final URI hubUrl;

        protected ContentFetchWork(final DataStore dataStore, final FeedFetcher feedFetcher, final URI hubUrl)
        {
            this.dataStore = dataStore;
            this.feedFetcher = feedFetcher;
            this.hubUrl = hubUrl;
        }

        public void run()
        {
            // we ignore the result here and rely on the feed fetcher event EVENT_TYPE_FEED_RETRIEVED to fire if the
            // feed has been actually retrieved from the web instead of being just read from cache
            try
            {
                PerRequestUserAgentHttpClientFeedFetcher.setRequestUserAgent(String.format(
                    Constants.USER_AGENT_FORMAT, hubUrl, dataStore.getTotalSubscriberCount(hubUrl)));
                feedFetcher.retrieveFeed(hubUrl.toURL());
            }
            catch (final Exception e)
            {
                LOG.error("Failed to fetch content from: " + hubUrl, e);
            }
            finally
            {
                PerRequestUserAgentHttpClientFeedFetcher.removeRequestUserAgent();
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
        private final DataStore dataStore;
        protected final ContentDistributionContext contentDistributionContext;

        protected DistributeContentRetryCallback(final MuleContext muleContext,
                                                 final DataStore dataStore,
                                                 final ContentDistributionContext contentDistributionContext)
        {
            this.muleContext = muleContext;
            this.dataStore = dataStore;
            this.contentDistributionContext = contentDistributionContext;
        }

        public String getWorkDescription()
        {
            return "Distributing content to " + contentDistributionContext.getCallbackUrl();
        }

        public void doWork(final RetryContext context) throws Exception
        {
            final Map<String, String> headers = new HashMap<String, String>();
            addHeaders(headers);

            final MuleMessage response = new MuleClient(muleContext).send(
                contentDistributionContext.getCallbackUrl().toString(),
                contentDistributionContext.getPayload(), headers, (int) Constants.SUBSCRIBER_TIMEOUT_MILLIS);

            if (response == null)
            {
                throw new TimeoutException("Failed to send content to: "
                                           + contentDistributionContext.getCallbackUrl());
            }

            final String getResponseStatusCode = response.getInboundProperty(
                HttpConnector.HTTP_STATUS_PROPERTY, "nil");

            if (!StringUtils.startsWith(getResponseStatusCode, "2"))
            {
                throw new IllegalArgumentException("Received status " + getResponseStatusCode + " from: "
                                                   + contentDistributionContext.getCallbackUrl());
            }

            final String onBehalfOf = response.getInboundProperty(Constants.HUB_ON_BEHALF_OF_HEADER, "");
            if (StringUtils.isNotBlank(onBehalfOf))
            {
                final int subscriberCount = Integer.valueOf(onBehalfOf);
                dataStore.storeSubscriberCount(contentDistributionContext.getTopicUrl(),
                    contentDistributionContext.getCallbackUrl(), subscriberCount);
                LOG.info("Successfully distributed content to " + subscriberCount + " subscriber(s) at: "
                         + contentDistributionContext.getCallbackUrl());
            }
            else
            {
                LOG.info("Successfully distributed content to: "
                         + contentDistributionContext.getCallbackUrl());
            }
        }

        protected void addHeaders(final Map<String, String> headers)
        {
            headers.put(HttpConstants.HEADER_CONTENT_TYPE, contentDistributionContext.getContentType());
        }
    }

    public static final class DistributeAuthenticatedContentRetryCallback extends
        DistributeContentRetryCallback
    {
        private final String signature;

        protected DistributeAuthenticatedContentRetryCallback(final MuleContext muleContext,
                                                              final DataStore dataStore,
                                                              final ContentDistributionContext contentDistributionContext,
                                                              final byte[] secret) throws Exception
        {
            super(muleContext, dataStore, contentDistributionContext);
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
            final byte[] rawHmac = mac.doFinal(contentDistributionContext.getPayload().getBytes());
            return new String(Base64.encodeBase64(rawHmac));
        }
    }

    public static class ContentDistributionContext
    {
        private final URI topicUrl;
        private final String contentType;
        private final String payload;
        private final URI callbackUrl;

        protected ContentDistributionContext(final URI topicUrl,
                                             final String contentType,
                                             final String payload,
                                             final URI callbackUrl)
        {
            this.topicUrl = topicUrl;
            this.contentType = contentType;
            this.payload = payload;
            this.callbackUrl = callbackUrl;
        }

        public URI getTopicUrl()
        {
            return topicUrl;
        }

        public String getContentType()
        {
            return contentType;
        }

        public String getPayload()
        {
            return payload;
        }

        public URI getCallbackUrl()
        {
            return callbackUrl;
        }
    }

    private final FeedFetcher feedFetcher;

    public PublisherHandler(final MuleContext muleContext,
                            final DataStore dataStore,
                            final RetryPolicyTemplate retryPolicyTemplate)
    {
        super(muleContext, dataStore, retryPolicyTemplate);

        feedFetcher = new PerRequestUserAgentHttpClientFeedFetcher(dataStore);
        feedFetcher.setPreserveWireFeed(true);
        feedFetcher.addFetcherEventListener(this);
    }

    @Override
    public HubResponse handle(final Map<String, List<String>> formParams)
    {
        final List<URI> hubUrls = HubUtils.getMandatoryUrlParameters(Constants.HUB_URL_PARAM, formParams);
        for (final URI hubUrl : hubUrls)
        {
            try
            {
                getMuleContext().getWorkManager().scheduleWork(
                    new ContentFetchWork(getDataStore(), feedFetcher, hubUrl));
            }
            catch (final Exception e)
            {
                final String errorMessage = "Failed to schedule content fetch for: " + hubUrl;
                getLogger().error(errorMessage, e);
                return HubResponse.serverError(errorMessage);
            }
        }

        return HubResponse.noContent();
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
            final String payload = createDistributedPayload(feed, newEntries);
            final String contentType = getDistributedContentType(feed);
            distributeContent(topicUrl, contentType, payload, topicSubscriptions);
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

    private String createDistributedPayload(final SyndFeed templateFeed, final List<SyndEntry> newEntries)
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

    private void distributeContent(final URI topicUrl,
                                   final String contentType,
                                   final String payload,
                                   final Set<TopicSubscription> topicSubscriptions) throws Exception
    {
        for (final TopicSubscription topicSubscription : topicSubscriptions)
        {
            final ContentDistributionContext contentDistributionContext = new ContentDistributionContext(
                topicUrl, contentType, payload, topicSubscription.getCallbackUrl());

            if (topicSubscription.getSecret() != null)
            {
                getRetryPolicyTemplate().execute(
                    new DistributeAuthenticatedContentRetryCallback(getMuleContext(), getDataStore(),
                        contentDistributionContext, topicSubscription.getSecret()),
                    getMuleContext().getWorkManager());
            }
            else
            {
                getRetryPolicyTemplate().execute(
                    new DistributeContentRetryCallback(getMuleContext(), getDataStore(),
                        contentDistributionContext), getMuleContext().getWorkManager());
            }
        }
    }
}
