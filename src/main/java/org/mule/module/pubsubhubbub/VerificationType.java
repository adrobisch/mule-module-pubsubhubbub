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
        public Response verify(final AbstractVerifiableRequest request, final MuleContext muleContext)
        {

            attemptVerification(request, muleContext);
            return Response.noContent().build();
        }
    },
    ASYNC
    {
        @Override
        public Response verify(final AbstractVerifiableRequest request, final MuleContext muleContext)
        {
            // FIXME implement asynchronous verification
            return Response.status(Status.ACCEPTED).build();
        }
    };

    private final static Log LOG = LogFactory.getLog(VerificationType.class);

    public static VerificationType parse(final String s)
    {
        try
        {
            return valueOf(StringUtils.upperCase(s));
        }
        catch (final IllegalArgumentException iae)
        {
            // rethrow with a less technical message, as it is routed back to the caller
            throw new IllegalArgumentException("Unsupported verification mode: " + s, iae);
        }
    }

    public abstract Response verify(AbstractVerifiableRequest request, MuleContext muleContext);

    private static void attemptVerification(final AbstractVerifiableRequest request,
                                            final MuleContext muleContext)
    {
        final String verificationChallenge = UUID.randomUUID().toString();

        try
        {
            final URI verificationUrl = buildVerificationUrl(request, verificationChallenge);

            final MuleMessage response = new MuleClient(muleContext).request(verificationUrl.toString(),
                Constants.VERIFICATION_TIMEOUT_MILLIS);

            validateResponse(response, request, verificationChallenge);

            // FIXME store validated subscription or unsubscription
        }
        catch (final URISyntaxException use)
        {
            throw new RuntimeException("Failed to build verification URL for: " + request, use);
        }
        catch (final MuleException me)
        {
            throw new RuntimeException("Failed to call verification URL for: " + request, me);
        }
    }

    private static void validateResponse(final MuleMessage response,
                                         final AbstractVerifiableRequest request,
                                         final String verificationChallenge)
    {
        if (response == null)
        {
            throw new IllegalStateException("No response has been received during verification of: "
                                            + request);
        }

        final String payload = getPayloadAsString(response, request);

        if (!StringUtils.equals(payload, verificationChallenge))
        {
            LOG.warn("Got challenge: " + payload + ", expecting: " + verificationChallenge + ", for: "
                     + request);

            throw new IllegalArgumentException("Wrong value for verification challenge of: " + request);
        }
    }

    private static String getPayloadAsString(final MuleMessage response,
                                             final AbstractVerifiableRequest request)
    {
        try
        {
            return response.getPayloadAsString();
        }
        catch (final Exception e)
        {
            throw new RuntimeException("Failed to retrieve verification response URL for: " + request, e);
        }
    }

    private static URI buildVerificationUrl(final AbstractVerifiableRequest request,
                                            final String verificationChallenge) throws URISyntaxException
    {
        final StringBuilder queryBuilder = new StringBuilder(
            StringUtils.defaultString(request.getCallbackUrl().getQuery()));

        appendToQuery(Constants.HUB_MODE_PARAM, request.getMode(), queryBuilder);
        appendToQuery(Constants.HUB_TOPIC_PARAM, request.getTopicUrl().toString(), queryBuilder);
        appendToQuery(Constants.HUB_CHALLENGE_PARAM, verificationChallenge, queryBuilder);
        appendToQuery(Constants.HUB_LEASE_SECONDS_PARAM, Long.toString(request.getLeaseSeconds()),
            queryBuilder);

        if (StringUtils.isNotBlank(request.getVerificationToken()))
        {
            appendToQuery(Constants.HUB_VERIFY_TOKEN_PARAM, request.getVerificationToken(), queryBuilder);
        }

        final URI callbackUrl = request.getCallbackUrl();
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
