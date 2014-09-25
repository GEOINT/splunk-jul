splunk-jul
==========

Simple java.util.logging formatter to output meaningful logs into a 
log format that Splunk can read natively.

The StandardSplunkFormatter can be configured for use like any other log 
formatter, use the LogManager javadoc for more information.

The formatter converts the majority of the LogRecord fields to standard 
splunk fields, with the exception of the StackTraceElements from the 
Throwable (if there is one).  The stack trace is added as a JSON array, so that 
it can be searched using the [spath] 
(http://docs.splunk.com/Documentation/Splunk/4.3.1/SearchReference/Spath) 
command within the splunk query syntax.

