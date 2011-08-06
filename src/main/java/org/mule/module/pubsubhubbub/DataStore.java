/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.pubsubhubbub;

import java.net.URI;

import org.mule.api.store.ObjectDoesNotExistException;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreException;

public class DataStore
{
    private ObjectStore<TopicSubscription> subscriptionStore;

    public void setSubscriptionStore(final ObjectStore<TopicSubscription> subscriptionStore)
    {
        this.subscriptionStore = subscriptionStore;
    }

    public void storeTopicSubscription(final TopicSubscription subscription)
    {
        final URI topicUrl = subscription.getTopicUrl();

        try
        {
            // not atomic :(
            if (subscriptionStore.contains(topicUrl))
            {
                subscriptionStore.remove(topicUrl);
            }

            subscriptionStore.store(topicUrl, subscription);
        }
        catch (final ObjectStoreException ose)
        {
            throw new RuntimeException("Failed to store: " + subscription, ose);
        }
    }

    public TopicSubscription getTopicSubscription(final URI topicUrl)
    {
        try
        {
            return subscriptionStore.retrieve(topicUrl);
        }
        catch (final ObjectDoesNotExistException odnee)
        {
            return null;
        }
        catch (final ObjectStoreException ose)
        {
            throw new RuntimeException("Failed to retrieve topic subscription at " + topicUrl, ose);
        }
    }
}
