/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.pubsubhubbub.data;

import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mule.api.store.ObjectDoesNotExistException;
import org.mule.api.store.ObjectStoreException;
import org.mule.api.store.PartitionableObjectStore;

import com.sun.syndication.fetcher.impl.FeedFetcherCache;
import com.sun.syndication.fetcher.impl.SyndFeedInfo;

public class DataStore implements FeedFetcherCache
{
    private static final String TOPIC_SUBSCRIPTION_CALLBACKS_PARTITION = "TopicSubscriptionCallbacks";
    private static final String TOPIC_FEED_IDS_PARTITION = "TopicFeedEntryIds";
    private static final String FEED_FETCHER_CACHE_PARTITION = "FeedFetcherCache";

    private PartitionableObjectStore<Serializable> objectStore;

    public void setObjectStore(final PartitionableObjectStore<Serializable> objectStore)
    {
        this.objectStore = objectStore;
    }

    public void storeTopicSubscription(final TopicSubscription subscription)
    {
        storeInSet(subscription.getTopicUrl(), subscription, TOPIC_SUBSCRIPTION_CALLBACKS_PARTITION);
    }

    public void removeTopicSubscription(final TopicSubscription subscription)
    {
        removeFromSet(subscription.getTopicUrl(), subscription, TOPIC_SUBSCRIPTION_CALLBACKS_PARTITION);
    }

    @SuppressWarnings("unchecked")
    public Set<TopicSubscription> getTopicSubscriptions(final URI topicUrl)
    {
        final Set<TopicSubscription> result = new HashSet<TopicSubscription>();

        for (final TopicSubscription topicSubscription : (Set<TopicSubscription>) retrieve(topicUrl,
            TOPIC_SUBSCRIPTION_CALLBACKS_PARTITION, (Serializable) Collections.EMPTY_SET))
        {
            if (topicSubscription.isExpired())
            {
                removeTopicSubscription(topicSubscription);
            }
            else
            {
                result.add(topicSubscription);
            }
        }

        return result;
    }

    public void storeTopicFeedId(final URI topicUrl, final String feedId)
    {
        storeInSet(topicUrl, feedId, TOPIC_FEED_IDS_PARTITION);
    }

    @SuppressWarnings("unchecked")
    public Set<String> getTopicFeedIds(final URI topicUrl)
    {
        return (Set<String>) retrieve(topicUrl, TOPIC_FEED_IDS_PARTITION,
            (Serializable) Collections.EMPTY_SET);
    }

    // Support for Rome feed fetcher
    public SyndFeedInfo getFeedInfo(final URL feedUrl)
    {
        return (SyndFeedInfo) retrieve(feedUrl, FEED_FETCHER_CACHE_PARTITION, null);
    }

    public void setFeedInfo(final URL feedUrl, final SyndFeedInfo syndFeedInfo)
    {
        store(feedUrl, syndFeedInfo, FEED_FETCHER_CACHE_PARTITION);
    }

    public void clear()
    {
        flush(FEED_FETCHER_CACHE_PARTITION);
    }

    public SyndFeedInfo remove(final URL feedUrl)
    {
        return (SyndFeedInfo) remove(feedUrl, FEED_FETCHER_CACHE_PARTITION);
    }

    private void store(final Serializable key, final Serializable value, final String domain)
    {
        try
        {
            // not atomic :(
            if (objectStore.contains(key, domain))
            {
                objectStore.remove(key, domain);
            }

            objectStore.store(key, value, domain);
        }
        catch (final ObjectStoreException ose)
        {
            throw new RuntimeException("Failed to store: " + value, ose);
        }
    }

    private Serializable remove(final Serializable key, final String domain)
    {
        try
        {
            return objectStore.remove(key, domain);
        }
        catch (final ObjectDoesNotExistException odnee)
        {
            return null;
        }
        catch (final ObjectStoreException ose)
        {
            throw new RuntimeException("Failed to remove: " + key, ose);
        }
    }

    private void flush(final String domain)
    {
        try
        {
            // not atomic :(
            final List<Serializable> keys = objectStore.allKeys(domain);
            for (final Serializable key : keys)
            {
                remove(key, domain);
            }
        }
        catch (final ObjectStoreException ose)
        {
            throw new RuntimeException("Failed to flush: " + domain, ose);
        }
    }

    private void storeInSet(final Serializable key, final Serializable value, final String domain)
    {
        // not atomic :(
        @SuppressWarnings("unchecked")
        Set<Serializable> values = (Set<Serializable>) retrieve(key, domain, null);

        if (values == null)
        {
            values = new HashSet<Serializable>();
        }

        values.add(value);

        store(key, (Serializable) values, domain);
    }

    private void removeFromSet(final Serializable key, final Serializable value, final String domain)
    {
        // not atomic :(
        @SuppressWarnings("unchecked")
        final Set<Serializable> values = (Set<Serializable>) retrieve(key, domain,
            (Serializable) Collections.EMPTY_SET);

        if (values.isEmpty())
        {
            return;
        }

        values.remove(value);

        store(key, (Serializable) values, domain);
    }

    private Serializable retrieve(final Serializable key, final String domain, final Serializable defaultValue)
    {
        try
        {
            return objectStore.retrieve(key, domain);
        }
        catch (final ObjectDoesNotExistException odnee)
        {
            return defaultValue;
        }
        catch (final ObjectStoreException ose)
        {
            throw new RuntimeException("Failed to retrieve: " + key + " at " + domain, ose);
        }
    }

}
