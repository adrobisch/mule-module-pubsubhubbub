/**
 * Mule PubSubHubbub Connector
 *
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.pubsubhubbub.handler;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleContext;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.module.pubsubhubbub.HubResponse;
import org.mule.module.pubsubhubbub.data.DataStore;

/**
 * Defines a handler for processing hub requests.
 */
public abstract class AbstractHubActionHandler
{
    private final Log logger = LogFactory.getLog(getClass());

    private final MuleContext muleContext;

    private final DataStore dataStore;

    private final RetryPolicyTemplate retryPolicyTemplate;

    protected AbstractHubActionHandler(final MuleContext muleContext,
                                       final DataStore dataStore,
                                       final RetryPolicyTemplate retryPolicyTemplate)
    {
        Validate.notNull(muleContext, "muleContext can't be null");
        Validate.notNull(dataStore, "dataStore can't be null");
        Validate.notNull(retryPolicyTemplate, "retryPolicyTemplate can't be null");

        this.muleContext = muleContext;
        this.dataStore = dataStore;
        this.retryPolicyTemplate = retryPolicyTemplate;
    }

    public abstract HubResponse handle(final Map<String, List<String>> formParams);

    public MuleContext getMuleContext()
    {
        return muleContext;
    }

    public Log getLogger()
    {
        return logger;
    }

    public DataStore getDataStore()
    {
        return dataStore;
    }

    public RetryPolicyTemplate getRetryPolicyTemplate()
    {
        return retryPolicyTemplate;
    }

}
