package org.geoint.logging.splunk;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
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
    private static final DateTimeFormatter DATE_FORMATTER
            = DateTimeFormatter.ofPattern(DATE_FORMAT);

    private final List<SplunkNormalizer> normalizers = new ArrayList<>();

    public StandardSplunkFormatter() {
        //add default normalizers
        normalizers.add((raw) -> {
            //replaces double quotes with single quotes
            return raw.replace("\"", "'");
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
        ZonedDateTime datetime = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(lr.getMillis()), ZoneOffset.UTC
        );

        sb.append(DATE_FORMATTER.format(datetime));

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
                .append(escapeJson(value))
                .append(JSON_QUOTE);
    }

    /**
     * Escapes the provided raw string IAW RFC 4627.
     *
     * @param raw
     * @return escaped string IAW RFC 4627
     */
    private String escapeJson(String string) {
        //shamelessly copied from Jettison v1.3.7 (Apachev2).  props!

        if (string == null || string.length() == 0) {
            return "\"\"";
        }

        char c = 0;
        int i;
        int len = string.length();
        StringBuilder sb = new StringBuilder(len + 4);
        String t;

        sb.append('"');
        for (i = 0; i < len; i += 1) {
            c = string.charAt(i);
            switch (c) {
                case '\\':
                case '"':
                    sb.append('\\');
                    sb.append(c);
                    break;
                case '/':
                    if (i > 0 && string.charAt(i - 1) == '<') {
                        sb.append('\\');
                    }
                    sb.append(c);
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                default:
                    if (c < ' ') {
                        t = "000" + Integer.toHexString(c);
                        sb.append("\\u")
                                .append(t.substring(t.length() - 4));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
        return sb.toString();
    }

}
