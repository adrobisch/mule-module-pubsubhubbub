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

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

/**
 * Deals with subscription requests.
 */
public class HubSubscriptionHandler extends AbstractHubActionHandler
{
    @Override
    public Response handle(final MultivaluedMap<String, String> formParams)
    {
        final SubscriptionRequest subscriptionRequest = new SubscriptionRequest(formParams);
        return subscriptionRequest.getVerificationType().verify(subscriptionRequest, getMuleContext(),
            new Runnable()
            {
                @Override
                public void run()
                {
                    for (final TopicSubscription topicSubscription : subscriptionRequest.getTopicSubscriptions())
                    {
                        getDataStore().storeTopicSubscription(topicSubscription);
                    }
                }
            });
    }
}
