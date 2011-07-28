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

import java.util.Map;

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
    private Map<HubMode, AbstractHubActionHandler> handlers;

    @POST
    public Response handleRequest(final MultivaluedMap<String, String> formParams)
    {
        final HubMode hubMode = HubMode.valueOf(StringUtils.upperCase(formParams.getFirst(Constants.HUB_MODE_PARAM)));
        final AbstractHubActionHandler handler = handlers.get(hubMode);

        if (handler == null)
        {
            throw new UnsupportedOperationException(hubMode.toString());
        }

        return handler.handle(formParams);
    }

    public void setHandlers(final Map<HubMode, AbstractHubActionHandler> handlers)
    {
        this.handlers = handlers;
    }

    public Map<HubMode, AbstractHubActionHandler> getHandlers()
    {
        return handlers;
    }
}
