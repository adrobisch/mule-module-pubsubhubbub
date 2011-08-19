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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.annotations.Module;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.param.Default;
import org.mule.api.annotations.param.Optional;

// FIXME re-implement HubResource in here and drop dependency on Jersey in favor of DevKit
@Module(name = "pubsubhubbub", namespace = "http://www.mulesoft.org/schema/mule/pubsubhubbub", schemaLocation = "http://www.mulesoft.org/schema/mule/pubsubhubbub/3.2/mule-pubsubhubbub.xsd")
public class HubModule
{
    private static final Log LOGGER = LogFactory.getLog(HubModule.class);

    @Processor(name = "hub")
    public Object handleRequest(@Default("#[payload]") @Optional final Object payload)
    {
        LOGGER.info("Received payload: " + payload);
        return "OK";
    }
}
