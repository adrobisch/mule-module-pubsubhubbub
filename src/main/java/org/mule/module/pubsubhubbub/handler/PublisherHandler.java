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

import java.net.URI;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.mule.api.retry.RetryCallback;
import org.mule.api.retry.RetryContext;
import org.mule.module.pubsubhubbub.Constants;
import org.mule.module.pubsubhubbub.HubResource;

public class PublisherHandler extends AbstractHubActionHandler
{
    public static class ContentFetch implements RetryCallback
    {
        private final URI hubUrl;

        protected ContentFetch(final URI hubUrl)
        {
            this.hubUrl = hubUrl;
        }

        public String getWorkDescription()
        {
            return "Fetching content of: " + hubUrl;
        }

        public void doWork(final RetryContext context) throws Exception
        {
            // FIXME implement!
            throw new UnsupportedOperationException("NOT IMPLEMENTED YET");
        }
    }

    @Override
    public Response handle(final MultivaluedMap<String, String> formParams)
    {
        final List<URI> hubUrls = HubResource.getMandatoryUrlParameters(Constants.HUB_URL_PARAM, formParams);
        return Response.noContent().build();
    }
}
