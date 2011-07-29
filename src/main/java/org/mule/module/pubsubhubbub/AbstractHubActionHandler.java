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

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;

/**
 * Defines a handler for processing hub requests.
 */
public abstract class AbstractHubActionHandler implements MuleContextAware
{
    private MuleContext muleContext;

    public void setMuleContext(final MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    protected MuleContext getMuleContext()
    {
        return muleContext;
    }

    public abstract Response handle(final MultivaluedMap<String, String> formParams);
}
