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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.RandomStringUtils;
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
    private static final URI TEST_TOPIC_URI;
    static
    {
        try
        {
            TEST_TOPIC_URI = new URI(TEST_TOPIC);
        }
        catch (final URISyntaxException urise)
        {
            throw new RuntimeException(urise);
        }
    }

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

    public void testSubscriptionRequestWithTooBigASecret() throws Exception
    {
        final Map<String, String> subscriptionRequest = new HashMap<String, String>();
        subscriptionRequest.put("hub.mode", "subscribe");
        subscriptionRequest.put("hub.mode", "subscribe");
        subscriptionRequest.put("hub.callback", "http://localhost:" + getSubscriberCallbacksPort()
                                                + "/cb-failure");
        subscriptionRequest.put("hub.topic", TEST_TOPIC);
        subscriptionRequest.put("hub.verify", "sync");
        subscriptionRequest.put("hub.secret", RandomStringUtils.randomAlphanumeric(200));

        final MuleMessage response = sendRequestToHub(subscriptionRequest);
        assertEquals("400", response.getInboundProperty("http.status"));
        assertEquals("Maximum secret size is 200 bytes", response.getPayloadAsString());
    }

    public void testFailedSynchronousSubscriptionConfirmation() throws Exception
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
        doTestSuccessfullSynchronousSubscription();
    }

    public void testSuccessfullSynchronousSubscriptionWithVerifyToken() throws Exception
    {
        final Map<String, String> extraSubscriptionParam = Collections.singletonMap("hub.verify_token",
            RandomStringUtils.randomAlphanumeric(20));
        doTestSuccessfullSynchronousSubscription(extraSubscriptionParam);
    }

    public void testSuccessfullSynchronousSubscriptionWithQueryParamInCallback() throws Exception
    {
        doTestSuccessfullSynchronousSubscription("?foo=bar");
    }

    public void testSuccessfullSynchronousSubscriptionWithSecret() throws Exception
    {
        final Map<String, String> extraSubscriptionParam = Collections.singletonMap("hub.secret",
            RandomStringUtils.randomAlphanumeric(20));
        doTestSuccessfullSynchronousSubscription(extraSubscriptionParam);
    }

    private void doTestSuccessfullSynchronousSubscription() throws Exception
    {
        doTestSuccessfullSynchronousSubscription("");
    }

    private void doTestSuccessfullSynchronousSubscription(final Map<String, String> extraSubscriptionParam)
        throws Exception
    {
        dotestSuccessfullSynchronousSubscription(extraSubscriptionParam, "");
    }

    @SuppressWarnings("unchecked")
    private void doTestSuccessfullSynchronousSubscription(final String callbackQuery) throws Exception
    {
        dotestSuccessfullSynchronousSubscription(Collections.EMPTY_MAP, callbackQuery);
    }

    // TODO test multi-topic subscription
    // TODO test re-subscription

    private void dotestSuccessfullSynchronousSubscription(final Map<String, String> extraSubscriptionParam,
                                                          final String callbackQuery) throws Exception
    {
        final String callback = "http://localhost:" + getSubscriberCallbacksPort() + "/cb-success"
                                + callbackQuery;

        final Map<String, String> subscriptionRequest = new HashMap<String, String>();
        subscriptionRequest.put("hub.mode", "subscribe");
        subscriptionRequest.put("hub.callback", callback);
        subscriptionRequest.put("hub.topic", TEST_TOPIC);
        subscriptionRequest.put("hub.verify", "sync");
        subscriptionRequest.putAll(extraSubscriptionParam);

        final MuleMessage response = sendRequestToHub(subscriptionRequest);

        assertEquals("204", response.getInboundProperty("http.status"));

        final Map<String, List<String>> subscriberVerifyParams = TestUtils.getUrlParameters(getFunctionalTestComponent(
            "successfullSubscriberCallback").getLastReceivedMessage().toString());
        assertEquals("subscribe", subscriberVerifyParams.get("hub.mode").get(0));
        assertEquals(TEST_TOPIC, subscriberVerifyParams.get("hub.topic").get(0));
        assertTrue(StringUtils.isNotBlank(subscriberVerifyParams.get("hub.challenge").get(0)));
        assertTrue(NumberUtils.isDigits(subscriberVerifyParams.get("hub.lease_seconds").get(0)));

        final String verifyToken = subscriptionRequest.get("hub.verify_token");
        if (StringUtils.isNotBlank(verifyToken))
        {
            assertEquals(verifyToken, subscriberVerifyParams.get("hub.verify_token").get(0));
        }
        else
        {
            assertNull(subscriberVerifyParams.get("hub.verify_token"));
        }

        if (StringUtils.isNotBlank(callbackQuery))
        {
            final Map<String, List<String>> queryParams = TestUtils.getUrlParameters(callbackQuery);
            for (final Entry<String, List<String>> queryParam : queryParams.entrySet())
            {
                assertEquals(queryParam.getValue(), subscriberVerifyParams.get(queryParam.getKey()));
            }
        }
        else
        {
            assertNull(subscriberVerifyParams.get("foo"));
        }

        final DataStore dataStore = muleContext.getRegistry().lookupObject(DataStore.class);
        final TopicSubscription topicSubscription = dataStore.getTopicSubscription(TEST_TOPIC_URI);
        assertEquals(TEST_TOPIC_URI, topicSubscription.getTopicUrl());
        assertEquals(new URI(callback), topicSubscription.getCallbackUrl());
        assertTrue(topicSubscription.getExpiryTime() > 0L);
        final String secretAsString = subscriptionRequest.get("hub.secret");
        if (StringUtils.isNotBlank(secretAsString))
        {
            assertTrue(Arrays.equals(secretAsString.getBytes("utf-8"), topicSubscription.getSecret()));
        }
        else
        {
            assertNull(topicSubscription.getSecret());
        }
    }

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
