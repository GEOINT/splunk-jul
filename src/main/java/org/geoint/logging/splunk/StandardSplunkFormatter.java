package org.geoint.logging.splunk;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
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
    private static final char JSON_QUOTE = '\'';
    private static final char CLASS_METHOD_SEPARATOR = '#';
    private static final char KV_SEPARATOR = '=';
    private static final String FIELD_SEPARATOR = ", ";
    private static final char QUOTE = '"';
    private static final String DATE_FORMAT = "yyyy-MM-dd hh:mm:ss.SSS Z";
    private static final ThreadLocal<SimpleDateFormat> dateFormat
            = new ThreadLocal<SimpleDateFormat>() {
                @Override
                protected SimpleDateFormat initialValue() {
                    SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);
                    format.setTimeZone(TimeZone.getTimeZone("UTC"));
                    return format;
                }
            };
    private final List<SplunkNormalizer> normalizers = new ArrayList<>();

    public StandardSplunkFormatter() {
        //add default normalizers
        //TODO replace these with lambdas

        //replaces double quotes with single quotes
        normalizers.add(new SplunkNormalizer() {

            @Override
            public String normalize(String raw) {
                return raw.replace("\"", "'");
            }
        });
    }

    /**
     * Add a normalizer to the chain.
     *
     * @param normalizer
     * @return
     */
    public StandardSplunkFormatter normalizer(SplunkNormalizer normalizer) {
        normalizers.add(normalizer);
        return this;
    }

    @Override
    public String format(LogRecord lr) {
        StringBuilder sb = new StringBuilder();
        //TODO use javax.time 
        Calendar cal = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(lr.getMillis());
        sb.append(dateFormat.get().format(cal.getTime()));

        appendKV(sb, KEY_LEVEL, lr.getLevel().getName());//log level
        appendKV(sb, KEY_LOGGER, lr.getLoggerName());  //logger name
        appendKV(sb, KEY_MSG, formatMessage(lr)); //message
        appendKV(sb, KEY_SOURCE, lr.getSourceClassName()
                + CLASS_METHOD_SEPARATOR
                + lr.getSourceMethodName()); //log source class+method

        if (lr instanceof SplunkLogRecord) {
            for (Entry<String, String> field : ((SplunkLogRecord) lr).getFields()) {
                appendKV(sb, normalize(field.getKey()), field.getValue());
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

    private void appendKV(StringBuilder sb, String key, String value) {
        sb.append(FIELD_SEPARATOR)
                .append(key) //normally don't need to normalize keys -- do this manually as needed
                .append(KV_SEPARATOR)
                .append(QUOTE)
                .append(normalize(value))
                .append(QUOTE);
    }

    /**
     * "Escapes" splunk field IAW their best practices.
     *
     * Escaping, for splunk, is actually substitutions.
     *
     * @param value
     */
    private String normalize(String value) {
        //TODO 1.8 use streams
        for (SplunkNormalizer n : normalizers) {
            value = n.normalize(value);
        }
        return value;
    }

    private void formatException(Throwable ex, StringBuilder sb) {
        //exception class type is sometimes all we got
        appendKV(sb, KEY_EXCEPTION_CLASS, ex.getClass().getName());

        if (ex.getMessage() != null && !ex.getMessage().isEmpty()) {
            appendKV(sb, KEY_EXCEPTION_MSG, ex.getMessage());
        }

        StackTraceElement[] stack = ex.getStackTrace();
        if (stack != null && stack.length > 0) {
            sb.append(FIELD_SEPARATOR)
                    .append(KEY_STACK)
                    .append(KV_SEPARATOR)
                    .append(QUOTE)
                    .append(JSON_OPEN_ARRAY);
            for (int i = 0; i < stack.length; i++) {
                StackTraceElement ste = stack[i];

                //store stack trace data as JSON object
                sb.append(JSON_OPEN_OBJ);
                appendJsonKv(sb, KEY_STACK_CLASS, ste.getClassName()); //class name
                appendJsonKv(sb, KEY_STACK_METHOD, ste.getMethodName()); //method name
                appendJsonKv(sb, KEY_STACK_LINE, String.valueOf(ste.getLineNumber())); //line number
                sb.append(JSON_CLOSE_OBJ);//close object

                if (i + 1 != stack.length) {
                    //more stack
                    sb.append(JSON_FIELD_SEPARATOR);
                }
            }
            sb.append(JSON_CLOSE_ARRAY);
            sb.append(QUOTE);
        }
    }

    private void appendJsonKv(StringBuilder sb, String name, String value) {
        sb.append(JSON_QUOTE)
                .append(name)
                .append(JSON_QUOTE)
                .append(JSON_KV_SEPARATOR)
                .append(JSON_QUOTE)
                .append(value) //TODO escape for json + splunk
                .append(JSON_QUOTE);
    }

}
