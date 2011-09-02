Mule PubSubHubbub Module
========================

Allows Mule to act as a PubSubHubbub (aka PuSH) hub.

Supported PubSubHubbub Version
------------------------------

This module implements the [PubSubHubbub Core 0.3 -- Working Draft](http://pubsubhubbub.googlecode.com/svn/trunk/pubsubhubbub-core-0.3.html) spec.


Build Commands
--------------

To compile and install in the local Maven repository:

    mvn clean install  

Note that this will run the complete test suite, which contains numerous integration tests hence can take a little while. If it takes too much time, use:

    mvn -DskipTests clean install
    

Usage
-----

Add the PubSubHubbub module namespace declaration to your Mule configuration:

    ...
    xmlns:pubsubhubbub="http://www.mulesoft.org/schema/mule/pubsubhubbub"
    ...
    http://www.mulesoft.org/schema/mule/pubsubhubbub http://www.mulesoft.org/schema/mule/pubsubhubbub/3.2/mule-pubsubhubbub.xsd


Then configure the hub back-end store and retry policy:

    <spring:beans>
        <!-- Any implementation of org.mule.util.store.PartitionableObjectStore can be used as a back-end for the hub -->
        <spring:bean id="hubObjectStore"
            class="org.mule.util.store.PartitionedInMemoryObjectStore" />

        <!-- The policy template defines the hub behavior when it has to retry an operation like validating a subscription request -->
        <spring:bean id="hubRetryPolicyTemplate"
            class="org.mule.retry.async.AsynchronousRetryTemplate">
            <spring:constructor-arg>
                <spring:bean class="org.mule.retry.policies.SimpleRetryPolicyTemplate">
                    <!-- retry 12 times every 5 minutes-->
                    <spring:constructor-arg index="0"
                        value="300000" />
                    <spring:constructor-arg index="1"
                        value="12" />
                </spring:bean>
            </spring:constructor-arg>
        </spring:bean>
    </spring:beans>

    <pubsubhubbub:config objectStore-ref="hubObjectStore"
                         retryPolicyTemplate-ref="hubRetryPolicyTemplate" />


Exposing the hub to the outside world is then trivial:

    <flow name="hub">
        <http:inbound-endpoint address="http://localhost:8080/hub" />
        <pubsubhubbub:hub />
    </flow>

    
Implementation Status
---------------------

Supported:

- Subscription with Synchronous and Asynchronous Verification
- Unsubscription with Synchronous and Asynchronous Verification
- Publisher New Content Notification
- Content Fetch
- Content Distribution
- Authenticated Content Distribution
- Number of subscribers in user agent (including support of X-Hub-On-Behalf-Of)


Not Supported:

- [Aggregated Content Distribution](http://pubsubhubbub.googlecode.com/svn/trunk/pubsubhubbub-core-0.3.html#aggregatedistribution)

    
Known Limitations
-----------------

- Default lease length is fixed to 1 week
- Subscriber HTTP interactions time-out is fixed to 15 seconds
- The same retry policy is used for un/subscription verification, content fetch and delivery attempts
