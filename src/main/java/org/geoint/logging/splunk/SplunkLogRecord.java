package org.geoint.logging.splunk;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Adds key/value pair meta on the record to be included in a splunk event
 * record fields, so they are searchable on Splunk.
 */
public class SplunkLogRecord extends LogRecord {

    private final static long serialVersionUID = 1L;
    private final static String DEFAULT_FIELD_NAMESPACE = "fld_";
    private static String fieldNamespace = DEFAULT_FIELD_NAMESPACE;
    private final Map<String, String> fields = new HashMap<>();

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

}
