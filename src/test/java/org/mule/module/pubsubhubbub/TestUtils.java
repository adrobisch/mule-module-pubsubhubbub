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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class TestUtils
{
    // http://stackoverflow.com/questions/1667278/parsing-query-strings-in-java
    public static Map<String, List<String>> getUrlParameters(final String url)
        throws UnsupportedEncodingException
    {
        final Map<String, List<String>> params = new HashMap<String, List<String>>();
        final String[] urlParts = url.split("\\?");
        if (urlParts.length > 1)
        {
            final String query = urlParts[1];
            for (final String param : query.split("&"))
            {
                final String pair[] = param.split("=");
                final String key = URLDecoder.decode(pair[0], "UTF-8");
                final String value = URLDecoder.decode(pair[1], "UTF-8");
                List<String> values = params.get(key);
                if (values == null)
                {
                    values = new ArrayList<String>();
                    params.put(key, values);
                }
                values.add(value);
            }
        }
        return params;
    }
}
