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

import java.util.List;
import java.util.Map;

import org.mule.api.MuleContext;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.module.pubsubhubbub.HubResponse;
import org.mule.module.pubsubhubbub.data.DataStore;
import org.mule.module.pubsubhubbub.data.TopicSubscription;
import org.mule.module.pubsubhubbub.request.AbstractVerifiableRequest;
import org.mule.module.pubsubhubbub.request.SubscriptionRequest;

/**
 * Deals with subscription requests.
 */
public class SubscriptionHandler extends AbstractHubActionHandler
{
    public SubscriptionHandler(final MuleContext muleContext,
                               final DataStore dataStore,
                               final RetryPolicyTemplate retryPolicyTemplate)
    {
        super(muleContext, dataStore, retryPolicyTemplate);
    }

    @Override
    public HubResponse handle(final Map<String, List<String>> formParams)
    {
        final AbstractVerifiableRequest subscriptionRequest = new SubscriptionRequest(formParams);
        return subscriptionRequest.getVerificationType().verify(subscriptionRequest, this, new Runnable()
        {
            @Override
            public void run()
            {
                for (final TopicSubscription topicSubscription : subscriptionRequest.getTopicSubscriptions())
                {
                    getDataStore().storeTopicSubscription(topicSubscription);
                    getLogger().info("Subscribed: " + topicSubscription);
                }
            }
        });
    }
}
