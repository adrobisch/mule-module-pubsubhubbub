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

import java.io.Serializable;
import java.net.URI;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class TopicSubscription implements Serializable
{
    private static final long serialVersionUID = 1L;

    private final URI callbackUrl;

    private final URI topicUrl;

    private final long expiryTime;

    private final byte[] secret;

    protected TopicSubscription(final URI callbackUrl,
                                final URI topicUrl,
                                final long expiryTime,
                                final byte[] secret)
    {
        this.callbackUrl = callbackUrl;
        this.topicUrl = topicUrl;
        this.expiryTime = expiryTime;
        this.secret = secret;
    }

    @Override
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    public URI getCallbackUrl()
    {
        return callbackUrl;
    }

    public URI getTopicUrl()
    {
        return topicUrl;
    }

    public long getExpiryTime()
    {
        return expiryTime;
    }

    public byte[] getSecret()
    {
        return secret;
    }
}
