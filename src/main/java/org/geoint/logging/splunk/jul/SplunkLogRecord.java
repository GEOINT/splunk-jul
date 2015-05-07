package org.geoint.logging.splunk.jul;

import org.geoint.logging.splunk.crypto.EventHash;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.geoint.logging.splunk.SplunkEvent;

/**
 * LogRecord interface to a SplunkEvent.
 *
 * Adds key/value pair meta on the record to be included in a splunk event
 * record fields, so they are searchable on Splunk.
 */
public class SplunkLogRecord extends LogRecord implements SplunkEvent {

    private static final long serialVersionUID = 2L;

    private SplunkEvent event;
    private Map<String, String> fields = new HashMap<>();

    private static final String CONST_NULL = "null";

    private static final Logger logger
            = Logger.getLogger("org.geoint.logging.splunk");

    public SplunkLogRecord(Level level, String msg) {
        super(level, msg);
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
        if (name == null) {
        }
        fields.put(name, value);
        event = null;
        return this;
    }

    @Override
    public ZonedDateTime getEventTime() {
        updateEvent();
        return event.getEventTime();
    }

    @Override
    public EventHash getHash() {
        updateEvent();
        return event.getHash();
    }

    @Override
    public Map<String, String> getFields() {
        updateEvent();
        return event.getFields();
    }

    @Override
    public String getFieldValue(String field) {
        updateEvent();
        return event.getFieldValue(field);
    }

    @Override
    public Set<String> getFieldNames() {
        updateEvent();
        return event.getFieldNames();
    }

    @Override
    public void setSourceMethodName(String sourceMethodName) {
        super.setSourceMethodName(sourceMethodName);
        event = null;
    }

    @Override
    public void setSourceClassName(String sourceClassName) {
        super.setSourceClassName(sourceClassName);
        event = null;
    }

    @Override
    public void setThrown(Throwable thrown) {
        super.setThrown(thrown);
        event = null;
    }

    @Override
    public void setMillis(long millis) {
        super.setMillis(millis);
        event = null;
    }

    @Override
    public void setMessage(String message) {
        super.setMessage(message);
        event = null;
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        event = null;
    }

    @Override
    public void setLoggerName(String name) {
        super.setLoggerName(name);
        event = null;
    }

    @Override
    public String asString() {
        updateEvent();
        return StandardSplunkFormatter.DEFAULT.format(this);
    }

    @Override
    public String toString() {
        return asString();
    }

    private void updateEvent() {
        if (event != null) {
            return;
        }
        event = SplunkLogUtil.toEvent(this, fields);
        fields.putAll(event.getFields());
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
