package org.geoint.logging.splunk;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Adds key/value pair meta on the record to be included in a splunk event
 * record fields, so they are searchable on Splunk.
 */
public class SplunkLogRecord extends LogRecord {

    private final static long serialVersionUID = 2L;
    private final static String DEFAULT_FIELD_NAMESPACE = "fld_";
    private final static String CONST_NULL = "null";
    private static String fieldNamespace = DEFAULT_FIELD_NAMESPACE;
    private final Map<String, String> fields = new HashMap<>();
    private final static Logger logger
            = Logger.getLogger("org.geoint.logging.splunk.record");

    public SplunkLogRecord(Level level, String msg) {
        super(level, msg);
    }

    public static void setFieldNamespace(String ns) {
        synchronized (SplunkLogRecord.class) {
            fieldNamespace = ns;
        }
    }

    /**
     * Add a meta field that will be included as a splunk event field.
     *
     * Each meta field will, by default, be appended with a namespace to ensure
     * there is a unique field name. This can be changed globally by calling the {@link #setFieldNamespace(java.lang.String)
     * } method.
     *
     * @param name
     * @param value
     * @return fluid interface, returns itself
     */
    public SplunkLogRecord field(String name, String value) {
        if (name != null) {
            fields.put(fieldNamespace + name, value);
        }
        return this;
    }

    /**
     * Returns the field entries for this record.
     *
     * @return
     */
    public Set<Entry<String, String>> getFields() {
        return fields.entrySet();
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
