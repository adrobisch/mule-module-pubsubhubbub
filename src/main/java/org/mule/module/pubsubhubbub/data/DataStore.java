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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.mule.api.store.ObjectDoesNotExistException;
import org.mule.api.store.ObjectStoreException;
import org.mule.api.store.PartitionableObjectStore;

public class DataStore
{
    private static final String TOPIC_SUBSCRIPTION_CALLBACKS_PARTITION = "TopicSubscriptionCallbacks";

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
