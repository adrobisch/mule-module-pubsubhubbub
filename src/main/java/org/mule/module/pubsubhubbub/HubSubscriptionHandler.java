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

import java.net.URL;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Deals with subscription and unsubscription requests.
 */
public class HubSubscriptionHandler implements HubActionHandler
{
    private final static Log LOG = LogFactory.getLog(HubSubscriptionHandler.class);

    @Override
    public Response handle(final MultivaluedMap<String, String> formParams)
    {
        final URL callbackUrl = HubResource.getMandatoryUrlParameter(Constants.HUB_CALLBACK_PARAM, formParams);
        final URL topicUrl = HubResource.getMandatoryUrlParameter(Constants.HUB_TOPIC_PARAM, formParams);
        final SubscriptionVerificationMode verificationMode = getSubscriptionVerificationMode(formParams);

        // TODO support optional parameters

        // FIXME implement!
        throw new UnsupportedOperationException("NOT IMPLEMENTED YET");
    }

    private SubscriptionVerificationMode getSubscriptionVerificationMode(final MultivaluedMap<String, String> formParams)
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
                return SubscriptionVerificationMode.valueOf(StringUtils.upperCase(verificationMode));
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
