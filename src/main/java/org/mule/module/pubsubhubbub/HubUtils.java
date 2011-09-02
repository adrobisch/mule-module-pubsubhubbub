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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

public abstract class HubUtils
{
    private HubUtils()
    {
        throw new UnsupportedOperationException("do not instantiate");
    }

    public static String getFirstValue(final Map<String, List<String>> formParams, final String key)
    {
        final List<String> values = formParams.get(key);
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    public static String getMandatoryStringParameter(final String name,
                                                     final Map<String, List<String>> formParams)
    {
        final String value = getFirstValue(formParams, name);

        if (StringUtils.isEmpty(value))
        {
            throw new IllegalArgumentException("Missing mandatory parameter: " + name);
        }

        return value;
    }

    public static URI getMandatoryUrlParameter(final String name, final Map<String, List<String>> formParams)
    {
        final String value = getMandatoryStringParameter(name, formParams);

        try
        {
            final URI uri = new URI(value);

            if (StringUtils.isNotEmpty(uri.getFragment()))
            {
                throw new IllegalArgumentException("Fragment found in URL parameter: " + name);
            }

            return uri;
        }
        catch (final URISyntaxException use)
        {
            use.printStackTrace();
            throw new IllegalArgumentException("Invalid URL parameter: " + name, use);
        }
    }

    public static List<URI> getMandatoryUrlParameters(final String name,
                                                      final Map<String, List<String>> formParams)
    {

        final List<String> values = formParams.get(name);

        if ((values == null) || (values.isEmpty()))
        {
            throw new IllegalArgumentException("Missing mandatory parameter: " + name);
        }

        try
        {
            final List<URI> uris = new ArrayList<URI>();

            for (final String value : values)
            {
                if (StringUtils.isBlank(value))
                {
                    continue;
                }

                final URI uri = new URI(value);

                if (StringUtils.isNotEmpty(uri.getFragment()))
                {
                    throw new IllegalArgumentException("Fragment found in URL parameter: " + name);
                }

                uris.add(uri);
            }

            if (uris.isEmpty())
            {
                throw new IllegalArgumentException("No value found for: " + name);
            }

            return uris;
        }
        catch (final URISyntaxException use)
        {
            use.printStackTrace();
            throw new IllegalArgumentException("Invalid URL parameter: " + name, use);
        }
    }

}
