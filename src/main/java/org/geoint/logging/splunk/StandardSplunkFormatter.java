package org.geoint.logging.splunk;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Formats a {@link LogRecord} into a basic format Splunk can ingest by default.
 *
 * Most log record fields are stored as simple key/value fields and will be
 * extracted by the standard splunk log reader. The exception stack trace is
 * stored in JSON format so that it can be searched using the splunk
 * {@code spath} command
 * {@link http://docs.splunk.com/Documentation/Splunk/4.3.1/SearchReference/Spath}
 * without any additional configuration.
 *
 *
 */
public class StandardSplunkFormatter extends Formatter {

    private static final String KEY_LEVEL = "level";
    private static final String KEY_LOGGER = "logger";
    private static final String KEY_MSG = "message";
    private static final String KEY_SOURCE = "source";
    private static final String KEY_EXCEPTION_CLASS = "exClass";
    private static final String KEY_EXCEPTION_MSG = "exMsg";
    private static final String KEY_STACK = "stack";
    private static final String KEY_STACK_CLASS = "class";
    private static final String KEY_STACK_METHOD = "method";
    private static final String KEY_STACK_LINE = "line";

    private static final char JSON_OPEN_ARRAY = '[';
    private static final char JSON_CLOSE_ARRAY = ']';
    private static final char JSON_OPEN_OBJ = '{';
    private static final char JSON_CLOSE_OBJ = '}';
    private static final char JSON_KV_SEPARATOR = ':';
    private static final char JSON_FIELD_SEPARATOR = ',';
    private static final char CLASS_METHOD_SEPARATOR = '#';
    private static final char KV_SEPARATOR = '=';
    private static final String FIELD_SEPARATOR = " ";
    private static final char QUOTE = '"';
    private static final String DATE_FORMAT = "yyyy-MM-dd hh:mm:ss.SSS Z";
    private static final ThreadLocal<SimpleDateFormat> dateFormat
            = new ThreadLocal<SimpleDateFormat>() {
                @Override
                protected SimpleDateFormat initialValue() {
                    SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);
                    format.setTimeZone(TimeZone.getTimeZone("GMT"));
                    return format;
                }
            };

    @Override
    public String format(LogRecord lr) {
        StringBuilder sb = new StringBuilder();
        sb.append(dateFormat.get().format(new Date(lr.getMillis())))
                .append(FIELD_SEPARATOR)
                //log level
                .append(KEY_LEVEL).append(KV_SEPARATOR)
                .append(QUOTE).append(lr.getLevel().getName()).append(QUOTE)
                .append(FIELD_SEPARATOR)
                //logger name
                .append(KEY_LOGGER).append(KV_SEPARATOR)
                .append(QUOTE).append(lr.getLoggerName()).append(QUOTE)
                .append(FIELD_SEPARATOR)
                //message
                .append(KEY_MSG).append(KV_SEPARATOR)
                .append(QUOTE).append(formatMessage(lr)).append(QUOTE)
                .append(FIELD_SEPARATOR)
                //log source class+method
                .append(KEY_SOURCE).append(KV_SEPARATOR)
                .append(QUOTE).append(lr.getSourceClassName())
                .append(CLASS_METHOD_SEPARATOR).append(lr.getSourceMethodName())
                .append(QUOTE);

        if (lr instanceof SplunkLogRecord) {
            for (Entry<String, String> field : ((SplunkLogRecord) lr).getFields()) {
                sb.append(FIELD_SEPARATOR)
                        .append(field.getKey())
                        .append(KV_SEPARATOR)
                        .append(field.getValue());
            }
        }

        Throwable ex = lr.getThrown();
        if (ex != null) {
            //should the cause lineage also be included?
            //if so, we could wrap not just the stack trace but the whole
            //exception chain in JSON
            formatException(ex, sb);
        }

        sb.append(System.lineSeparator());
        return sb.toString();
    }

    private void formatException(Throwable ex, StringBuilder sb) {
        //exception class type is sometimes all we got
        sb.append(FIELD_SEPARATOR)
                .append(KEY_EXCEPTION_CLASS).append(KV_SEPARATOR)
                .append(QUOTE).append(ex.getClass().getName()).append(QUOTE);

        if (ex.getMessage() != null && !ex.getMessage().isEmpty()) {
            sb.append(FIELD_SEPARATOR)
                    .append(KEY_EXCEPTION_MSG).append(KV_SEPARATOR)
                    .append(QUOTE).append(ex.getMessage()).append(QUOTE);
        }
        StackTraceElement[] stack = ex.getStackTrace();
        if (stack != null && stack.length > 0) {
            sb.append(FIELD_SEPARATOR).append(KEY_STACK).append(KV_SEPARATOR)
                    .append(JSON_OPEN_ARRAY);
            for (int i = 0; i < stack.length; i++) {
                StackTraceElement ste = stack[i];

                //store stack trace data as JSON object
                sb.append(JSON_OPEN_OBJ)
                        //class name
                        .append(QUOTE).append(KEY_STACK_CLASS).append(QUOTE)
                        .append(JSON_KV_SEPARATOR)
                        .append(QUOTE).append(ste.getClassName()).append(QUOTE)
                        .append(JSON_FIELD_SEPARATOR)
                        //method name
                        .append(QUOTE).append(KEY_STACK_METHOD).append(QUOTE)
                        .append(JSON_KV_SEPARATOR)
                        .append(QUOTE).append(ste.getMethodName()).append(QUOTE)
                        .append(JSON_FIELD_SEPARATOR)
                        //line number
                        .append(QUOTE).append(KEY_STACK_LINE).append(QUOTE)
                        .append(JSON_KV_SEPARATOR)
                        .append(QUOTE).append(ste.getLineNumber()).append(QUOTE)
                        //close object
                        .append(JSON_CLOSE_OBJ);

                if (i + 1 != stack.length) {
                    //more stack
                    sb.append(JSON_FIELD_SEPARATOR);
                }
            }
            sb.append(JSON_CLOSE_ARRAY);
        }
    }

}
