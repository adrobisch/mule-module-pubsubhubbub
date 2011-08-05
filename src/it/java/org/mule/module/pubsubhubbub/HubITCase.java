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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.DynamicPortTestCase;
import org.mule.transport.http.HttpConstants;

public class HubITCase extends DynamicPortTestCase
{
    private static final String TEST_TOPIC = "http://mulesoft.org/fake-topic";
    private MuleClient muleClient;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        muleClient = new MuleClient(muleContext);
    }

    @Override
    protected int getNumPortsToFind()
    {
        return 2;
    }

    @Override
    protected String getConfigResources()
    {
        return "push-tests-config.xml";
    }

    public void testBadHubRequest() throws Exception
    {
        final Map<String, String> subscriptionRequest = new HashMap<String, String>();
        subscriptionRequest.put("hub.mode", "foo");

        final MuleMessage response = sendRequestToHub(subscriptionRequest);
        assertEquals("400", response.getInboundProperty("http.status"));
        assertEquals("Unsupported hub mode: foo", response.getPayloadAsString());
    }

    public void testBadSubscriptionRequest() throws Exception
    {
        final Map<String, String> subscriptionRequest = new HashMap<String, String>();
        subscriptionRequest.put("hub.mode", "subscribe");
        // missing all other required parameters

        final MuleMessage response = sendRequestToHub(subscriptionRequest);
        assertEquals("400", response.getInboundProperty("http.status"));
        assertEquals("Missing mandatory parameter: hub.callback", response.getPayloadAsString());
    }

    public void testFailedSynchronousSubscription() throws Exception
    {
        final Map<String, String> subscriptionRequest = new HashMap<String, String>();
        subscriptionRequest.put("hub.mode", "subscribe");
        subscriptionRequest.put("hub.callback", "http://localhost:" + getSubscriberCallbacksPort()
                                                + "/cb-failure");
        subscriptionRequest.put("hub.topic", TEST_TOPIC);
        subscriptionRequest.put("hub.verify", "sync");

        final MuleMessage response = sendRequestToHub(subscriptionRequest);
        assertEquals("500", response.getInboundProperty("http.status"));
    }

    public void testSuccessfullSynchronousSubscription() throws Exception
    {
        final Map<String, String> subscriptionRequest = new HashMap<String, String>();
        subscriptionRequest.put("hub.mode", "subscribe");
        subscriptionRequest.put("hub.callback", "http://localhost:" + getSubscriberCallbacksPort()
                                                + "/cb-success");
        subscriptionRequest.put("hub.topic", TEST_TOPIC);
        subscriptionRequest.put("hub.verify", "sync");

        final MuleMessage response = sendRequestToHub(subscriptionRequest);

        assertEquals("204", response.getInboundProperty("http.status"));

        final Map<String, List<String>> subscriberVerifyParams = TestUtils.getUrlParameters(getFunctionalTestComponent(
            "successfullSubscriberCallback").getLastReceivedMessage().toString());
        assertEquals("subscribe", subscriberVerifyParams.get("hub.mode").get(0));
        assertEquals(TEST_TOPIC, subscriberVerifyParams.get("hub.topic").get(0));
        assertTrue(StringUtils.isNotBlank(subscriberVerifyParams.get("hub.challenge").get(0)));
        assertTrue(NumberUtils.isDigits(subscriberVerifyParams.get("hub.lease_seconds").get(0)));
    }

    // TODO test secret and extra query string propagation

    private MuleMessage sendRequestToHub(final Map<String, String> subscriptionRequest) throws MuleException
    {
        final MuleMessage response = muleClient.send("http://localhost:" + getHubPort() + "/push/hub",
            subscriptionRequest,
            Collections.singletonMap(HttpConstants.HEADER_CONTENT_TYPE, "application/x-www-form-urlencoded"));
        return response;
    }

    private Integer getHubPort()
    {
        return getPorts().get(0);
    }

    private Integer getSubscriberCallbacksPort()
    {
        return getPorts().get(1);
    }
}
