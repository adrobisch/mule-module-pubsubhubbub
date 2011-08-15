/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.pubsubhubbub.handler;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.module.pubsubhubbub.data.DataStore;

/**
 * Defines a handler for processing hub requests.
 */
public abstract class AbstractHubActionHandler implements MuleContextAware
{
    private MuleContext muleContext;

    private DataStore dataStore;

    // TODO support different retry policies for subsciption and publication workflows
    private RetryPolicyTemplate retryPolicyTemplate;

    public void setMuleContext(final MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    public MuleContext getMuleContext()
    {
        return muleContext;
    }

    public void setDataStore(final DataStore dataStore)
    {
        this.dataStore = dataStore;
    }

    public DataStore getDataStore()
    {
        return dataStore;
    }

    public void setRetryPolicyTemplate(final RetryPolicyTemplate retryPolicyTemplate)
    {
        this.retryPolicyTemplate = retryPolicyTemplate;
    }

    public RetryPolicyTemplate getRetryPolicyTemplate()
    {
        return retryPolicyTemplate;
    }

    public abstract Response handle(final MultivaluedMap<String, String> formParams);
}
