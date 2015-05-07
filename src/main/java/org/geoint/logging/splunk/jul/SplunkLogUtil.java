package org.geoint.logging.splunk.jul;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.LogRecord;
import org.geoint.logging.splunk.NativeSplunkFormatter;
import org.geoint.logging.splunk.SplunkEvent;
import org.geoint.logging.splunk.crypto.EventHash;
import org.geoint.logging.splunk.crypto.MessageDigestRecordHasher;
import org.geoint.logging.splunk.json.Json;

/**
 *
 */
public class SplunkLogUtil {

    public static final String KEY_LEVEL = "level";
    public static final String KEY_LOGGER = "logger";
    public static final String KEY_MSG = "message";
    public static final String KEY_SOURCE = "source";
    public static final String KEY_TIME = "eventTime";
    public static final String KEY_EXCEPTION_CLASS = "exClass";
    public static final String KEY_EXCEPTION_MSG = "exMsg";
    public static final String KEY_STACK = "stack";
    public static final String KEY_STACK_CLASS = "class";
    public static final String KEY_STACK_METHOD = "method";
    public static final String KEY_STACK_LINE = "line";
    public static final String KEY_HASH = "hash";
    public static final char CLASS_METHOD_SEPARATOR = '#';
    /**
     * JVM property name which can be used to override the default field prefix
     * used.
     */
    public static final String PROPERTY_FIELD_PREFIX
            = "org.geoint.logging.splunk.record.prefix";
    private static final String DEFAULT_FIELD_PREFIX = "fld_";
    public static final String FIELD_PREFIX
            = System.getProperty(PROPERTY_FIELD_PREFIX, DEFAULT_FIELD_PREFIX);

    /**
     * convert to SplunkEvent
     *
     * @param lr
     * @return splunk event
     */
    public static SplunkEvent toEvent(LogRecord lr) {
        return SplunkEventImpl.fromLog(lr);
    }

    public static SplunkEvent toEvent(LogRecord lr,
            Map<String, String> addlFields) {
        if (lr instanceof SplunkEvent) {
            return (SplunkEvent) lr;
        }
        SplunkEventImpl event = SplunkEventImpl.fromLog(lr);
        event.fields.putAll(addlFields);
        return event;
    }

    static String prefixedFieldName(String fieldName) {
        return FIELD_PREFIX + fieldName;
    }

    /**
     * simple wrapper around a LogRecord.
     */
    private static class SplunkEventImpl implements SplunkEvent {

        private final ZonedDateTime eventTime;
        private final EventHash eventHash;
        private final Map<String, String> fields;

        private SplunkEventImpl(ZonedDateTime eventTime,
                Map<String, String> fields) {
            this.eventTime = eventTime;
            this.fields = fields;
            this.eventHash = hash();
            this.fields.put(prefixedFieldName(KEY_HASH), eventHash.asHex());
        }

        public static SplunkEventImpl fromLog(LogRecord lr) {

            ZonedDateTime eventTime = ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(lr.getMillis()),
                    ZoneOffset.UTC);

            //set fields
            Map<String, String> fields = new HashMap<>();
            fields.put(prefixedFieldName(KEY_TIME), ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(lr.getMillis()),
                    ZoneOffset.UTC).toString());
            fields.put(prefixedFieldName(KEY_LEVEL), lr.getLevel().getName());
            fields.put(prefixedFieldName(KEY_LOGGER), lr.getLoggerName());
            fields.put(prefixedFieldName(KEY_MSG), lr.getMessage());
            fields.put(prefixedFieldName(KEY_SOURCE), lr.getSourceClassName()
                    + CLASS_METHOD_SEPARATOR
                    + lr.getSourceMethodName());
            Throwable ex = lr.getThrown();
            if (ex != null) {
                fields.put(prefixedFieldName(KEY_EXCEPTION_CLASS),
                        ex.getClass().getName());

                if (ex.getMessage() != null && !ex.getMessage().isEmpty()) {
                    fields.put(prefixedFieldName(KEY_EXCEPTION_MSG),
                            ex.getMessage());
                }

                fields.put(prefixedFieldName(KEY_STACK),
                        exceptionStack(ex));
            }

            return new SplunkEventImpl(eventTime, fields);
        }

        private static String exceptionStack(Throwable ex) {
            //exception class type is sometimes all we got
            //add stack trace as JSON
            StackTraceElement[] stack = ex.getStackTrace();
            if (stack != null && stack.length > 0) {
                final Json stackJson = Json.asArray(
                        (json, se) -> {
                            json.element(KEY_STACK_CLASS, se.getClassName());//class name
                            json.element(KEY_STACK_METHOD, se.getMethodName());//method name
                            json.element(KEY_STACK_LINE, String.valueOf(se.getLineNumber())); //line number
                        },
                        stack);
                return stackJson.toString();
            }
            return "";
        }

        @Override
        public ZonedDateTime getEventTime() {
            return eventTime;
        }

        @Override
        public EventHash getHash() {
            return eventHash;
        }

        @Override
        public Map<String, String> getFields() {
            return Collections.unmodifiableMap(fields);
        }

        @Override
        public String getFieldValue(String field) {
            return getFields().get(field);
        }

        @Override
        public Set<String> getFieldNames() {
            return Collections.unmodifiableSet(getFields().keySet());
        }

        @Override
        public String asString() {
            return new NativeSplunkFormatter().format(this);
        }

        private EventHash hash() {
            return MessageDigestRecordHasher.sha256(this);
        }
    }
}
