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

- Synchronous Subscription
- Synchronous Unsubscription
- Asynchronous Subscription and Unsubscription

Pending:

- Publisher Support
