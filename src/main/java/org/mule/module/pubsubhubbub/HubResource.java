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

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;

/**
 * Resource that is the main entry point to the hub.
 */
@Path("/hub")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.TEXT_PLAIN)
public class HubResource
{
    private SubscriptionHandler subscriptionHandler;

    @POST
    public Response handleRequest(final MultivaluedMap<String, String> formParams)
    {
        final HubMode hubMode = HubMode.valueOf(StringUtils.upperCase(formParams.getFirst(Constants.HUB_MODE_PARAM)));

        switch (hubMode)
        {
            case SUBSCRIBE :
                subscriptionHandler.subscribe(formParams);
                break;

            case UNSUBSCRIBE :
                subscriptionHandler.unsubscribe(formParams);
                break;

            // FIXME add support for PUBLISH

            default :
                throw new UnsupportedOperationException(hubMode.toString());
        }

        return Response.noContent().build();
    }

    public void setSubscriptionHandler(final SubscriptionHandler subscriptionHandler)
    {
        this.subscriptionHandler = subscriptionHandler;
    }

    public SubscriptionHandler getSubscriptionHandler()
    {
        return subscriptionHandler;
    }
}
