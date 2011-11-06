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

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class Constants
{
    public static String USER_AGENT_FORMAT = "Mule PubSubHubbub Hub (%s; %d subscribers)";

    public static String HUB_SIGNATURE_HEADER = "X-Hub-Signature";
    public static String HUB_ON_BEHALF_OF_HEADER = "X-Hub-On-Behalf-Of";

    public static String WWW_FORM_URLENCODED_CONTENT_TYPE = "application/x-www-form-urlencoded";
    public static String RSS_CONTENT_TYPE = "application/rss+xml";
    public static String ATOM_CONTENT_TYPE = "application/atom+xml";

    public static final long SUBSCRIBER_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(15L);

    public static final int MAXIMUM_SECRET_SIZE = 200;

    public static final String CHARSET_MEDIA_TYPE_PARAM = "charset";
    public static final String DEFAULT_CHARSET = Charset.defaultCharset().toString();

    // Common Parameters
    public static final String REQUEST_ENCODING_PARAM = "request.encoding";

    // Mandatory
    public static final String HUB_MODE_PARAM = "hub.mode";

    // Subscription Parameters
    public static final String HUB_DEFAULT_LEASE_SECONDS_PARAM = "hub.default_lease_seconds";
    // Mandatory
    public static final String HUB_VERIFY_PARAM = "hub.verify";
    public static final String HUB_TOPIC_PARAM = "hub.topic";
    public static final String HUB_CALLBACK_PARAM = "hub.callback";
    public static final String HUB_CHALLENGE_PARAM = "hub.challenge";
    // Optional
    public static final String HUB_LEASE_SECONDS_PARAM = "hub.lease_seconds";
    public static final String HUB_SECRET_PARAM = "hub.secret";
    public static final String HUB_VERIFY_TOKEN_PARAM = "hub.verify_token";

    // Publisher Parameters
    // Mandatory
    public static final String HUB_URL_PARAM = "hub.url";

    public static final Set<String> SUPPORTED_MULTIVALUED_PARAMS = new HashSet<String>(Arrays.asList(
        Constants.HUB_VERIFY_PARAM, Constants.HUB_TOPIC_PARAM, Constants.HUB_URL_PARAM));
}
