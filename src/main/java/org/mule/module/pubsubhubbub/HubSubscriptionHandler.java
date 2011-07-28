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

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

/**
 * Deals with subscription and unsubscription requests.
 */
public class HubSubscriptionHandler extends AbstractHubActionHandler
{
    @Override
    public Response handle(final MultivaluedMap<String, String> formParams)
    {
        final URL callbackUrl = getMadatoryUrlParameter(Constants.HUB_CALLBACK_PARAM, formParams);
        final URL topicUrl = getMadatoryUrlParameter(Constants.HUB_TOPIC_PARAM, formParams);
        final String verify = getMandatoryParameter(Constants.HUB_VERIFY_PARAM, formParams);

        // TODO support optional parameters

        // FIXME implement!
        throw new UnsupportedOperationException("NOT IMPLEMENTED YET");
    }
}
