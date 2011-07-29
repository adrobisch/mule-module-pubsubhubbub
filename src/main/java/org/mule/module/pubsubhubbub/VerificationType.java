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
import java.net.URISyntaxException;
import java.util.UUID;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;

public enum VerificationType

{
    SYNC
    {
        @Override
        public Response verify(final SubscriptionRequest subscriptionRequest, final MuleContext muleContext)
        {

            attemptVerification(subscriptionRequest, muleContext);
            return Response.noContent().build();
        }
    },
    ASYNC
    {
        @Override
        public Response verify(final SubscriptionRequest subscriptionRequest, final MuleContext muleContext)
        {
            // FIXME implement!
            return Response.status(Status.ACCEPTED).build();
        }
    };

    private final static Log LOG = LogFactory.getLog(VerificationType.class);

    public abstract Response verify(SubscriptionRequest subscriptionRequest, MuleContext muleContext);

    private static void attemptVerification(final SubscriptionRequest subscriptionRequest,
                                            final MuleContext muleContext)
    {
        final String verificationChallenge = UUID.randomUUID().toString();

        try
        {
            final URI verificationUrl = buildVerificationUrl(subscriptionRequest, verificationChallenge);

            final MuleMessage response = new MuleClient(muleContext).request(verificationUrl.toString(),
                Constants.VERIFICATION_TIMEOUT_MILLIS);

            validateResponse(response, subscriptionRequest, verificationChallenge);
        }
        catch (final URISyntaxException use)
        {
            throw new RuntimeException("Failed to build verification URL for: " + subscriptionRequest, use);
        }
        catch (final MuleException me)
        {
            throw new RuntimeException("Failed to call verification URL for: " + subscriptionRequest, me);
        }
    }

    private static void validateResponse(final MuleMessage response,
                                         final SubscriptionRequest subscriptionRequest,
                                         final String verificationChallenge)
    {
        if (response == null)
        {
            throw new IllegalStateException("No response has been received during verification of: "
                                            + subscriptionRequest);
        }

        final String payload = getPayloadAsString(response, subscriptionRequest);

        if (!StringUtils.equals(payload, verificationChallenge))
        {
            LOG.warn("Got challenge: " + payload + ", expecting: " + verificationChallenge + ", for: "
                     + subscriptionRequest);

            throw new IllegalArgumentException("Wrong value for verification challenge of: "
                                               + subscriptionRequest);
        }
    }

    private static String getPayloadAsString(final MuleMessage response,
                                             final SubscriptionRequest subscriptionRequest)
    {
        try
        {
            return response.getPayloadAsString();
        }
        catch (final Exception e)
        {
            throw new RuntimeException("Failed to retrieve verification response URL for: "
                                       + subscriptionRequest, e);
        }
    }

    private static URI buildVerificationUrl(final SubscriptionRequest subscriptionRequest,
                                            final String verificationChallenge) throws URISyntaxException
    {
        final StringBuilder queryBuilder = new StringBuilder(
            StringUtils.defaultString(subscriptionRequest.getCallbackUrl().getQuery()));

        appendToQuery(Constants.HUB_MODE_PARAM, subscriptionRequest.getMode(), queryBuilder);
        appendToQuery(Constants.HUB_TOPIC_PARAM, subscriptionRequest.getTopicUrl().toString(), queryBuilder);
        appendToQuery(Constants.HUB_CHALLENGE_PARAM, verificationChallenge, queryBuilder);
        appendToQuery(Constants.HUB_LEASE_SECONDS_PARAM,
            Long.toString(subscriptionRequest.getLeaseSeconds()), queryBuilder);

        if (StringUtils.isNotBlank(subscriptionRequest.getVerificationToken()))
        {
            appendToQuery(Constants.HUB_VERIFY_TOKEN_PARAM, subscriptionRequest.getVerificationToken(),
                queryBuilder);
        }

        final URI callbackUrl = subscriptionRequest.getCallbackUrl();
        return new URI(callbackUrl.getScheme(), callbackUrl.getUserInfo(), callbackUrl.getHost(),
            callbackUrl.getPort(), callbackUrl.getPath(), queryBuilder.toString(), null);
    }

    private static void appendToQuery(final String name, final String value, final StringBuilder queryBuilder)
    {
        if (queryBuilder.length() != 0)
        {
            queryBuilder.append("&");
        }

        queryBuilder.append(name).append("=").append(value);
    }
}
