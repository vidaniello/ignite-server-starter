Info
==========
This simple project aims to simplify the start of an ignite server instance, moving all the configuration inside a 
java class, [specifically this one](https://github.com/vidaniello/ignite-server-starter/blob/main/src/main/java/com/github/vidaniello/igniteserver/IgniteNode.java), in addition it exposes a series of commands to restart, stop and query the service, 
both from the line of command (unix or windows) or from a web browser.

In test execution, from the IDE for example, to point to the correct maven resources parsed files (bin/*), a VM property must be specified, the -DprependTestUserDir=... property. For example, put -DprependTestUserDir=target/classes, which is the maven destination set by default as 'outputDirectory'. This property is not required in execution other than a test one.