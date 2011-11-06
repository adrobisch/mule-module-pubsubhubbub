/**
 * Mule PubSubHubbub Connector
 *
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.pubsubhubbub.data;

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

    public TopicSubscription(final URI callbackUrl,
                             final URI topicUrl,
                             final long expiryTime,
                             final byte[] secret)
    {
        this.callbackUrl = callbackUrl;
        this.topicUrl = topicUrl;
        this.expiryTime = expiryTime;
        this.secret = secret;
    }

    public boolean isExpired()
    {
        return System.currentTimeMillis() >= expiryTime;
    }

    @Override
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((callbackUrl == null) ? 0 : callbackUrl.hashCode());
        result = prime * result + ((topicUrl == null) ? 0 : topicUrl.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final TopicSubscription other = (TopicSubscription) obj;
        if (callbackUrl == null)
        {
            if (other.callbackUrl != null) return false;
        }
        else if (!callbackUrl.equals(other.callbackUrl)) return false;
        if (topicUrl == null)
        {
            if (other.topicUrl != null) return false;
        }
        else if (!topicUrl.equals(other.topicUrl)) return false;
        return true;
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
