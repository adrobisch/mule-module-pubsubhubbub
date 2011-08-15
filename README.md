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

To run the test suite:

    mvn -Pit verify
    
    
Implementation Status
---------------------

Done:

- Subscription with Synchronous and Asynchronous Verification
- Unsubscription with Synchronous and Asynchronous Verification

Pending:

- New Content Notification
- Content Fetch
- Content Distribution
- Authenticated Content Distribution
- Number of subscribers in user agent (including support of X-Hub-On-Behalf-Of)

Not Supported:

- Aggregated Content Distribution

    
Know Limitations
----------------

- Default lease length is fixed to 1 week
- Verification request time-out if fixed to 15 seconds
- The same retry policy is used for un/subscription verification, content fetch and delivery attempts
