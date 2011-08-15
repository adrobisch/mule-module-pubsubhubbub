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

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.mule.module.pubsubhubbub.data.TopicSubscription;
import org.mule.module.pubsubhubbub.request.AbstractVerifiableRequest;
import org.mule.module.pubsubhubbub.request.UnsubscriptionRequest;

/**
 * Deals with subscription requests.
 */
public class UnsubscriptionHandler extends AbstractHubActionHandler
{
    @Override
    public Response handle(final MultivaluedMap<String, String> formParams)
    {
        final AbstractVerifiableRequest unsubscriptionRequest = new UnsubscriptionRequest(formParams);
        return unsubscriptionRequest.getVerificationType().verify(unsubscriptionRequest, this, new Runnable()
        {
            @Override
            public void run()
            {
                for (final TopicSubscription topicSubscription : unsubscriptionRequest.getTopicSubscriptions())
                {
                    getDataStore().removeTopicSubscription(topicSubscription);
                    getLogger().info("Unsubscribed: " + topicSubscription);
                }
            }
        });
    }
}
