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

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;

public abstract class AbstractHubActionHandler
{
    public abstract Response handle(final MultivaluedMap<String, String> formParams);

    protected String getMandatoryParameter(final String name, final MultivaluedMap<String, String> formParams)
    {
        final String value = formParams.getFirst(name);

        if (StringUtils.isEmpty(value))
        {
            throw new IllegalArgumentException("Missing mandatory parameter: " + name);
        }

        return value;
    }

    protected URL getMadatoryUrlParameter(final String name, final MultivaluedMap<String, String> formParams)
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
