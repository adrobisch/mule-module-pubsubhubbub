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

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.RequestContext;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Module;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.param.Payload;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.api.store.PartitionableObjectStore;
import org.mule.api.transformer.TransformerMessagingException;
import org.mule.module.pubsubhubbub.data.DataStore;
import org.mule.module.pubsubhubbub.handler.AbstractHubActionHandler;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.transformers.HttpRequestBodyToParamMap;

// FIXME re-implement HubResource in here and drop dependency on Jersey in favor of DevKit
@Module(name = "pubsubhubbub", namespace = "http://www.mulesoft.org/schema/mule/pubsubhubbub", schemaLocation = "http://www.mulesoft.org/schema/mule/pubsubhubbub/3.2/mule-pubsubhubbub.xsd")
public class HubModule implements MuleContextAware, Initialisable
{
    private static final Log LOGGER = LogFactory.getLog(HubModule.class);

    private MuleContext muleContext;

    @Configurable
    private PartitionableObjectStore<Serializable> objectStore;

    @Configurable
    private RetryPolicyTemplate retryPolicyTemplate;

    private DataStore dataStore;

    private HttpRequestBodyToParamMap httpRequestBodyToParamMap;

    private Map<HubMode, AbstractHubActionHandler> requestHandlers;

    public void initialise() throws InitialisationException
    {
        dataStore = new DataStore(objectStore);

        httpRequestBodyToParamMap = new HttpRequestBodyToParamMap();
        httpRequestBodyToParamMap.setMuleContext(muleContext);

        for (final HubMode hubMode : HubMode.values())
        {
            requestHandlers.put(hubMode, hubMode.newHandler(muleContext, dataStore, retryPolicyTemplate));
        }
    }

    @Processor(name = "hub")
    public MuleMessage handleRequest(@Payload final Object payload) throws MuleException
    {
        final MuleEvent muleEvent = RequestContext.getEvent();
        return handleRequest(muleEvent, payload).buildMuleMessage(muleContext);
    }

    private HubResponse handleRequest(final MuleEvent muleEvent, final Object payload) throws MuleException
    {
        final MuleMessage message = muleEvent.getMessage();

        if (!StringUtils.equalsIgnoreCase(message.getInboundProperty(HttpConnector.HTTP_METHOD_PROPERTY, ""),
            HttpConstants.METHOD_POST))
        {

            return HubResponse.badRequest("HTTP method must be: POST");
        }

        if (!StringUtils.startsWith(message.getInboundProperty(HttpConstants.HEADER_CONTENT_TYPE, ""),
            Constants.WWW_FORM_URLENCODED_CONTENT_TYPE))
        {
            return HubResponse.badRequest("Content type must be: application/x-www-form-urlencoded");
        }

        final Map<String, List<String>> parameters = getParameters(muleEvent);
        parameters.put(Constants.REQUEST_ENCODING_PARAM, Collections.singletonList(muleEvent.getEncoding()));

        try
        {
            return handleRequest(parameters);
        }
        catch (final IllegalArgumentException exception)
        {
            return HubResponse.badRequest(exception.getMessage());
        }
    }

    private HubResponse handleRequest(final Map<String, List<String>> parameters)
    {
        // FIXME implement!
        throw new UnsupportedOperationException("NOT IMPLEMENTED YET");
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<String>> getParameters(final MuleEvent muleEvent)
        throws TransformerMessagingException
    {
        final Map<String, Object> parameters = (Map<String, Object>) httpRequestBodyToParamMap.transform(
            muleEvent.getMessage(), muleEvent);

        final Map<String, List<String>> result = new HashMap<String, List<String>>();

        for (final Entry<String, Object> parameter : parameters.entrySet())
        {
            if (parameter.getValue() instanceof List<?>)
            {
                result.put(parameter.getKey(), (List<String>) parameter.getValue());
            }
            else
            {
                result.put(parameter.getKey(), Collections.singletonList(((String) parameter.getValue())));
            }
        }

        return result;
    }

    public void setMuleContext(final MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    public MuleContext getMuleContext()
    {
        return muleContext;
    }

    public void setObjectStore(final PartitionableObjectStore<Serializable> objectStore)
    {
        this.objectStore = objectStore;
    }

    public PartitionableObjectStore<Serializable> getObjectStore()
    {
        return objectStore;
    }

    public void setRetryPolicyTemplate(final RetryPolicyTemplate retryPolicyTemplate)
    {
        this.retryPolicyTemplate = retryPolicyTemplate;
    }

    public RetryPolicyTemplate getRetryPolicyTemplate()
    {
        return retryPolicyTemplate;
    }
}
