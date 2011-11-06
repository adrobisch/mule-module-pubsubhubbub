/**
 * Mule PubSubHubbub Connector
 *
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.pubsubhubbub.request;

import java.util.List;
import java.util.Map;

import org.mule.module.pubsubhubbub.HubMode;

public class SubscriptionRequest extends AbstractVerifiableRequest
{
    private static final long serialVersionUID = 1L;

    public SubscriptionRequest(final Map<String, List<String>> formParams)
    {
        super(formParams);
    }

    @Override
    public String getMode()
    {
        return HubMode.SUBSCRIBE.getMode();
    }
}
