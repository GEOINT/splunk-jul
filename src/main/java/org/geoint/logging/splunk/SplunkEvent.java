package org.geoint.logging.splunk;

import org.geoint.logging.splunk.crypto.EventHash;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Set;

/**
 * Essentially a map of fields.
 */
public interface SplunkEvent {

    /**
     * The time the event occurred.
     *
     * The event time will also be available as the [prefix_]eventTime field.
     *
     * @return time the event occurred
     */
    ZonedDateTime getEventTime();

    /**
     * The cryptographic hash signature of the event.
     *
     * <b>Rule Title:</b> The DBMS must protect audit data records and integrity
     * by using cryptographic mechanisms.
     * <b>STIG ID:</b> SRG-APP-000126-DB-000171
     *
     * @return current hash signature for the event
     */
    EventHash getHash();

    /**
     * Map of all the fields of the splunk event.
     *
     * @return immutable map of event fields
     */
    Map<String, String> getFields();

    /**
     *
     * @param field field name
     * @return value of the requested field
     */
    String getFieldValue(String field);

    /**
     *
     * @return immutable set of field names set for the event
     */
    Set<String> getFieldNames();

    /**
     *
     * @return event in a splunk-native format
     */
    String asString();
}
