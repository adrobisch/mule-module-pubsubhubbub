/**
 * Mule PubSubHubbub Connector
 *
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.lang.StringUtils;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;

public abstract class HubUtils
{
    private HubUtils()
    {
        throw new UnsupportedOperationException("do not instantiate");
    }

    public static String getFirstValue(final Map<String, List<String>> parameters, final String key)
    {
        final List<String> values = parameters.get(key);
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    public static String getFirstValue(final Map<String, List<String>> parameters,
                                       final String key,
                                       final String defaultKey)
    {
        final String value = getFirstValue(parameters, key);
        return value != null ? value : getFirstValue(parameters, defaultKey);
    }

    public static void setSingleValue(final Map<String, List<String>> parameters,
                                      final String key,
                                      final String value)
    {
        parameters.put(key, Collections.singletonList(value));
    }

    public static String getMandatoryStringParameter(final String name,
                                                     final Map<String, List<String>> parameters)
    {
        final String value = getFirstValue(parameters, name);

        if (StringUtils.isEmpty(value))
        {
            throw new IllegalArgumentException("Missing mandatory parameter: " + name);
        }

        return value;
    }

    public static URI getMandatoryUrlParameter(final String name, final Map<String, List<String>> parameters)
    {
        final String value = getMandatoryStringParameter(name, parameters);

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
                                                      final Map<String, List<String>> parameters)
    {

        final List<String> values = parameters.get(name);

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

    public static Map<String, List<String>> getHttpPostParameters(final MuleEvent muleEvent)
        throws MuleException, DecoderException
    {
        final Map<String, List<String>> params = new HashMap<String, List<String>>();
        addQueryStringToParameterMap(muleEvent.getMessageAsString(), params, muleEvent.getEncoding());
        return params;
    }

    // lifted from org.mule.transport.http.transformers.HttpRequestBodyToParamMap
    private static void addQueryStringToParameterMap(final String queryString,
                                                     final Map<String, List<String>> paramMap,
                                                     final String outputEncoding) throws DecoderException
    {
        final String[] pairs = queryString.split("&");
        for (final String pair : pairs)
        {
            final String[] nameValue = pair.split("=");
            if (nameValue.length == 2)
            {
                final URLCodec codec = new URLCodec(outputEncoding);
                final String key = codec.decode(nameValue[0]);
                final String value = codec.decode(nameValue[1]);
                addToParameterMap(paramMap, key, value);
            }
        }
    }

    // lifted from org.mule.transport.http.transformers.HttpRequestBodyToParamMap
    private static void addToParameterMap(final Map<String, List<String>> paramMap,
                                          final String key,
                                          final String value)
    {
        final List<String> existingValues = paramMap.get(key);

        if (existingValues != null)
        {
            existingValues.add(value);
        }
        else
        {
            final List<String> values = new ArrayList<String>();
            values.add(value);
            paramMap.put(key, values);
        }
    }
}
