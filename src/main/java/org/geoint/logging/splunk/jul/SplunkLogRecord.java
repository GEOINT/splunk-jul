package org.geoint.logging.splunk.jul;

import org.geoint.logging.splunk.crypto.EventHash;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.geoint.logging.splunk.SplunkEvent;
import org.geoint.logging.splunk.crypto.MessageDigestRecordHasher;

/**
 * LogRecord-based implementation of a SplunkEvent.
 *
 * Adds key/value pair meta on the record to be included in a splunk event
 * record fields, so they are searchable on Splunk.
 */
public class SplunkLogRecord extends LogRecord implements SplunkEvent {

    private static final long serialVersionUID = 2L;

    private final Map<String, String> fields = new TreeMap<>();
    private ZonedDateTime eventTime;
    private EventHash eventHash;
    private volatile boolean dirty;

    /**
     * JVM property name which can be used to override the default field prefix
     * used.
     */
    public static final String PROPERTY_FIELD_PREFIX
            = "org.geoint.logging.splunk.record.prefix";
    private static final String CONST_NULL = "null";
    private static final String KEY_LEVEL = "level";
    private static final String KEY_LOGGER = "logger";
    private static final String KEY_MSG = "message";
    private static final String KEY_SOURCE = "source";
    private static final String KEY_TIME = "eventTime";
    private static final String KEY_EXCEPTION_CLASS = "exClass";
    private static final String KEY_EXCEPTION_MSG = "exMsg";
    private static final String KEY_STACK = "stack";
    private static final String KEY_STACK_CLASS = "class";
    private static final String KEY_STACK_METHOD = "method";
    private static final String KEY_STACK_LINE = "line";
    private static final String KEY_HASH = "hash";

    private static final char JSON_OPEN_ARRAY = '[';
    private static final char JSON_CLOSE_ARRAY = ']';
    private static final char JSON_OPEN_OBJ = '{';
    private static final char JSON_CLOSE_OBJ = '}';
    private static final char JSON_KV_SEPARATOR = ':';
    private static final char JSON_FIELD_SEPARATOR = ',';
    private static final char JSON_QUOTE = '\'';
    private static final char KV_SEPARATOR = '=';
    private static final String FIELD_SEPARATOR = ", ";
    private static final char QUOTE = '"';
    private static final char CLASS_METHOD_SEPARATOR = '#';
    private static final String DEFAULT_FIELD_PREFIX = "fld_";
    private static final String FIELD_PREFIX
            = System.getProperty(PROPERTY_FIELD_PREFIX, DEFAULT_FIELD_PREFIX);
    private static final Logger logger
            = Logger.getLogger("org.geoint.logging.splunk");

    public SplunkLogRecord(Level level, String msg) {
        super(level, msg);

        fields.put(prefixedFieldName(KEY_LEVEL), this.getLevel().getName());//log level
        fields.put(prefixedFieldName(KEY_LOGGER), this.getLoggerName());  //logger name
        fields.put(prefixedFieldName(KEY_MSG), this.getMessage()); //message
        fields.put(prefixedFieldName(KEY_SOURCE), this.getSourceClassName()
                + CLASS_METHOD_SEPARATOR
                + this.getSourceMethodName()); //log source class+method

        Throwable ex = this.getThrown();
        if (ex != null) {
            //should the cause lineage also be included?
            //if so, we could wrap not just the stack trace but the whole
            //exception chain in JSON
            putExceptionFields(fields, ex);
        }
    }

    private void putExceptionFields(Map<String, String> flds,
            Throwable ex) {
//exception class type is sometimes all we got
       flds.put(KEY_EXCEPTION_CLASS, ex.getClass().getName());

        if (ex.getMessage() != null && !ex.getMessage().isEmpty()) {
            flds.put(KEY_EXCEPTION_MSG, ex.getMessage());
        }

        StringBuilder stJson = new StringBuilder();
        StackTraceElement[] stack = ex.getStackTrace();
        if (stack != null && stack.length > 0) {
            stJson.append(FIELD_SEPARATOR)
                    .append(KEY_STACK)
                    .append(KV_SEPARATOR)
                    .append(QUOTE)
                    
            for (int i = 0; i < stack.length; i++) {
                StackTraceElement ste = stack[i];

                //store stack trace data as JSON object
                sb.append(JSON_OPEN_ARRAY)
                        .append(JSON_OPEN_OBJ);
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

    private String prefixedFieldName(String fieldName) {
        return FIELD_PREFIX + fieldName;
    }

    /**
     * Add a meta field that will be included as a splunk event field.
     *
     * Each meta field will, by default, be appended with a namespace to ensure
     * there is a unique field name. This can be changed globally by calling the
     * {@link #setFieldNamespace(java.lang.String)} method.
     *
     * @param name
     * @param value
     * @return fluid interface, returns itself
     */
    public SplunkLogRecord field(String name, String value) {
        if (name != null) {
            fields.put(DEFAULT_FIELD_PREFIX + name, value);
            dirty = true;
        }
        return this;
    }

    @Override
    public ZonedDateTime getEventTime() {
        if (eventTime == null) {
            eventTime = ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(this.getMillis()),
                    ZoneOffset.UTC
            );
            dirty = true;
        }
        return eventTime;
    }

    @Override
    public EventHash getHash() {
        //TODO add configurability for hash type
        updateHash();
        return eventHash;
    }

    @Override
    public Map<String, String> getFields() {
        updateHash();
        return Collections.unmodifiableMap(fields);
    }

    @Override
    public String getFieldValue(String field) {
        updateHash();
        return fields.get(field);
    }

    @Override
    public Set<String> getFieldNames() {
        updateHash();
        return Collections.unmodifiableSet(fields.keySet());
    }

    @Override
    public String asString() {
        updateHash();
        return StandardSplunkFormatter.DEFAULT.format(this);
    }

    @Override
    public String toString() {
        return asString();
    }

    private void updateHash() {
        if (dirty || eventHash == null) {
            eventHash = MessageDigestRecordHasher.sha256(this);
        }
    }

//<code-fold desc="serializable methods" default="collapsed">
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeUTF(getLevel().getName());
        out.writeUTF(getLoggerName());
        out.writeUTF(getMessage());
        out.writeLong(getMillis());
        out.writeInt(getParameters().length);
        for (Object p : getParameters()) {
            out.writeObject(p);
        }
        out.writeUTF(getResourceBundleName());
        out.writeLong(getSequenceNumber());
        out.writeUTF(getSourceClassName());
        out.writeUTF(getSourceMethodName());
        out.writeInt(getThreadID());
        out.writeObject(getThrown());
        out.writeInt(fields.size());
        for (Entry<String, String> e : fields.entrySet()) {
            out.writeUTF(e.getKey());
            out.writeUTF((e.getValue() == null) ? CONST_NULL : e.getValue());
        }
    }

    private void readObject(ObjectInputStream in) throws IOException {
        setLevel(Level.parse(in.readUTF()));
        setLoggerName(in.readUTF());
        setMessage(in.readUTF());
        setMillis(in.readLong());
        final Object[] params = new Object[in.readInt()];
        for (int i = 0; i < params.length; i++) {
            try {
                params[i] = in.readObject();
            } catch (ClassNotFoundException ex) {
                logger.log(Level.SEVERE, "Unable to deserialize record parameter "
                        + i + " because the class was not found.", ex);
                params[i] = new Object(); //no body likes NPE's
            }
        }
        setResourceBundleName(in.readUTF());
        setSequenceNumber(in.readLong());
        setSourceClassName(in.readUTF());
        setSourceMethodName(in.readUTF());
        setThreadID(in.readInt());
        try {
            setThrown((Throwable) in.readObject());
        } catch (ClassNotFoundException ex) {
            logger.log(Level.SEVERE, "Unable to deserialize thrown record field, "
                    + "class was not found.", ex);
        }
        final int numFields = in.readInt();
        for (int i = 0; i < numFields; i++) {
            final String name = in.readUTF();
            final String value = in.readUTF();
            fields.put(name, (value.contentEquals(CONST_NULL) ? null : value));
        }
    }
//</code-fold>

}
