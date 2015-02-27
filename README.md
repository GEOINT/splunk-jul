splunk-jul
==========

Simple java.util.logging formatter to output meaningful logs into a 
log format that Splunk can read natively - no need to make any changes to 
the splunk forwarder configuration.

# How to Use

java.util.logging (JUL) has one really awesomething thing going for it: it's a 
standard part of Java.  If you've every worked complex, modular, applications 
you appreciate that - there is significantly less chance for version conflicts, 
classpath hell, etc.  With this in mind, we recommend you do not depend on this 
project in your code (unless you're extending it, of course).  Instead, use one 
of the various ways that (JUL) allows you to configure logging outside your 
code, such as with a configuration file.  For more information, check out the 
LogManager javadoc.

# Configuration

The StandardSplunkFormatter can be configured for use like any other log 
formatter, for example using a configuration file:

    java.util.logging.handlers=java.util.logging.FileHandler
    java.util.logging.FileHandler.formatter=org.geoint.logging.splunk.StandardSplunkFormatter
    java.util.logging.FileHandler.limit=2000000
    java.util.logging.FileHandler.count=5
    java.util.logging.FileHandler.pattern=/logs/geoint-coolstuff_%g.log
    java.util.logging.FileHandler.append=true

# Format Details   

The splunk formatter converts the majority of the LogRecord fields to fields 
that splunk readily understands without any configuration on the forwarder
or indexer.  For example, no need to worry about the datetime location or 
format, splunk will recognize it and index your events properly.

## StackTraceElement formatting

The hierarchy and collections within a Java stack trace is foreign to the 
much more simple key/value approach of splunk.  What we do with these is convert 
them to a format that is searchable by splunk (JSON).  Searching JSON 
contained with a splunk field has been supported since 4.3.1 using 
the [spath command] (http://docs.splunk.com/Documentation/Splunk/4.3.1/SearchReference/Spath) 
within the splunk query syntax.

