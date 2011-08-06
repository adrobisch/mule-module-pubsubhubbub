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
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;

public class SubscriptionRequest extends AbstractVerifiableRequest
{
    private static final long serialVersionUID = 1L;

    public SubscriptionRequest(final MultivaluedMap<String, String> formParams)
    {
        super(formParams);
    }

    @Override
    public String getMode()
    {
        return HubMode.SUBSCRIBE.getMode();
    }

    public List<TopicSubscription> getTopicSubscriptions()
    {
        final List<TopicSubscription> subscriptions = new ArrayList<TopicSubscription>();

        for (final URI topicUrl : getTopicUrls())
        {
            subscriptions.add(new TopicSubscription(getCallbackUrl(), topicUrl, getExpiryTime(), getSecret()));
        }

        return subscriptions;
    }
}
