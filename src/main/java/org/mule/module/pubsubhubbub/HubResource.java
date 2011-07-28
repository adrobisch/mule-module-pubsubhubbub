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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
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
    private Map<HubMode, HubActionHandler> handlers;

    @POST
    public Response handleRequest(final MultivaluedMap<String, String> formParams)
    {
        final HubMode hubMode = HubMode.valueOf(StringUtils.upperCase(getMandatoryParameter(
            Constants.HUB_MODE_PARAM, formParams)));

        final HubActionHandler handler = handlers.get(hubMode);

        if (handler == null)
        {
            throw new UnsupportedOperationException(hubMode.toString());
        }

        return handler.handle(formParams);
    }

    public void setHandlers(final Map<HubMode, HubActionHandler> handlers)
    {
        this.handlers = handlers;
    }

    public Map<HubMode, HubActionHandler> getHandlers()
    {
        return handlers;
    }

    public static String getMandatoryParameter(final String name,
                                               final MultivaluedMap<String, String> formParams)
    {
        final String value = formParams.getFirst(name);

        if (StringUtils.isEmpty(value))
        {
            throw new IllegalArgumentException("Missing mandatory parameter: " + name);
        }

        return value;
    }

    public static URL getMandatoryUrlParameter(final String name,
                                               final MultivaluedMap<String, String> formParams)
    {
        final String value = getMandatoryParameter(name, formParams);

        try
        {
            final URI uri = new URI(value);

            if (StringUtils.isNotEmpty(uri.getFragment()))
            {
                throw new IllegalArgumentException("Fragment found in URL parameter: " + name);
            }

            return uri.toURL();
        }
        catch (final URISyntaxException usi)
        {
            throw new IllegalArgumentException("Invalid URL parameter: " + name, usi);
        }
        catch (final MalformedURLException mue)
        {
            throw new IllegalArgumentException("Invalid URL parameter: " + name, mue);
        }
    }
}
