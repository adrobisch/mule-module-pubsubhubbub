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
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SubscriptionRequest
{
    private final static Log LOG = LogFactory.getLog(SubscriptionRequest.class);

    private final URI callbackUrl;
    private final URI topicUrl;
    private final long expiryTime;
    private final String secret;
    private final VerificationType verificationType;
    private final String verificationToken;

    public SubscriptionRequest(final MultivaluedMap<String, String> formParams)
    {
        callbackUrl = HubResource.getMandatoryUrlParameter(Constants.HUB_CALLBACK_PARAM, formParams);
        topicUrl = HubResource.getMandatoryUrlParameter(Constants.HUB_TOPIC_PARAM, formParams);
        expiryTime = System.currentTimeMillis() + 1000L * retrieveLeaseSeconds(formParams);
        secret = formParams.getFirst(Constants.HUB_SECRET_PARAM);
        verificationType = retrieveSubscriptionVerificationMode(formParams);
        verificationToken = formParams.getFirst(Constants.HUB_VERIFY_TOKEN_PARAM);
    }

    public String getMode()
    {
        return HubMode.SUBSCRIBE.getMode();
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

    public long getLeaseSeconds()
    {
        return (expiryTime - System.currentTimeMillis()) / 1000L;
    }

    public String getSecret()
    {
        return secret;
    }

    public VerificationType getVerificationType()
    {
        return verificationType;
    }

    public String getVerificationToken()
    {
        return verificationToken;
    }

    private long retrieveLeaseSeconds(final MultivaluedMap<String, String> formParams)
    {
        final String leaseSecondString = formParams.getFirst(Constants.HUB_LEASE_SECONDS_PARAM);

        if (StringUtils.isBlank(leaseSecondString))
        {
            return Constants.DEFAULT_LEASE_SECONDS;
        }

        return Long.valueOf(leaseSecondString);
    }

    private VerificationType retrieveSubscriptionVerificationMode(final MultivaluedMap<String, String> formParams)
    {
        final List<String> verificationModes = formParams.get(Constants.HUB_VERIFY_PARAM);

        if (verificationModes == null)
        {
            throw new IllegalArgumentException("Missing mandatory parameter: " + Constants.HUB_VERIFY_PARAM);
        }

        for (final String verificationMode : verificationModes)
        {
            try
            {
                return VerificationType.valueOf(StringUtils.upperCase(verificationMode));
            }
            catch (final IllegalArgumentException iae)
            {
                LOG.info("Ignoring unusupported verification mode: " + verificationMode);
            }
        }

        throw new IllegalArgumentException("No supported value found for parameter: "
                                           + Constants.HUB_VERIFY_PARAM);
    }
}
