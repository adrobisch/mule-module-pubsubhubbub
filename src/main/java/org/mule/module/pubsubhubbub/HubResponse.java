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

import java.util.Collections;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;

public class HubResponse
{
    private final int status;
    private final String body;

    private HubResponse(final int status, final String body)
    {
        this.status = status;
        this.body = body;
    }

    public MuleMessage buildMuleMessage(final MuleContext muleContext)
    {
        return new DefaultMuleMessage(body, Collections.singletonMap(HttpConnector.HTTP_STATUS_PROPERTY,
            (Object) status), muleContext);
    }

    public static HubResponse badRequest(final String message)
    {
        return new HubResponse(HttpConstants.SC_BAD_REQUEST, message);
    }

    public static HubResponse noContent()
    {
        return new HubResponse(HttpConstants.SC_NO_CONTENT, "");
    }

    public static HubResponse accepted()
    {
        return new HubResponse(HttpConstants.SC_ACCEPTED, "");
    }

    public static HubResponse serverError(final String message)
    {
        return new HubResponse(HttpConstants.SC_INTERNAL_SERVER_ERROR, message);
    }
}
