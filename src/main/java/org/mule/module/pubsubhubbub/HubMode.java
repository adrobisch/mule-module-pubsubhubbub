/**
 * Mule PubSubHubbub Connector
 *
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.pubsubhubbub;

import org.apache.commons.lang.StringUtils;
import org.mule.api.MuleContext;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.module.pubsubhubbub.data.DataStore;
import org.mule.module.pubsubhubbub.handler.AbstractHubActionHandler;
import org.mule.module.pubsubhubbub.handler.PublisherHandler;
import org.mule.module.pubsubhubbub.handler.SubscriptionHandler;
import org.mule.module.pubsubhubbub.handler.UnsubscriptionHandler;

public enum HubMode
{
    SUBSCRIBE
    {
        @Override
        public AbstractHubActionHandler newHandler(final MuleContext muleContext,
                                                   final DataStore dataStore,
                                                   final RetryPolicyTemplate retryPolicyTemplate)
        {
            return new SubscriptionHandler(muleContext, dataStore, retryPolicyTemplate);
        }
    },
    UNSUBSCRIBE
    {
        @Override
        public AbstractHubActionHandler newHandler(final MuleContext muleContext,
                                                   final DataStore dataStore,
                                                   final RetryPolicyTemplate retryPolicyTemplate)
        {
            return new UnsubscriptionHandler(muleContext, dataStore, retryPolicyTemplate);
        }
    },
    PUBLISH
    {
        @Override
        public AbstractHubActionHandler newHandler(final MuleContext muleContext,
                                                   final DataStore dataStore,
                                                   final RetryPolicyTemplate retryPolicyTemplate)
        {
            return new PublisherHandler(muleContext, dataStore, retryPolicyTemplate);
        }
    };

    public String getMode()
    {
        return StringUtils.lowerCase(this.toString());
    }

    public abstract AbstractHubActionHandler newHandler(MuleContext muleContext,
                                                        DataStore dataStore,
                                                        RetryPolicyTemplate retryPolicyTemplate);

    public static HubMode parse(final String s)
    {
        try
        {
            return valueOf(StringUtils.upperCase(s));
        }
        catch (final IllegalArgumentException iae)
        {
            // rethrow with a less technical message, as it is routed back to the caller
            throw new IllegalArgumentException("Unsupported hub mode: " + s, iae);
        }
    }
}
