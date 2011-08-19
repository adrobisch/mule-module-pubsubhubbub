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

    
Know Limitations
----------------

- Default lease length is fixed to 1 week
- Subscriber HTTP interactions time-out is fixed to 15 seconds
- The same retry policy is used for un/subscription verification, content fetch and delivery attempts
