package org.geoint.logging.splunk.jul;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.geoint.logging.splunk.SplunkEvent;
import static org.geoint.logging.splunk.jul.SplunkLogUtil.prefixedFieldName;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 */
public class SplunkLogUtilTest {

    /**
     * Test conversion from a LogRecord to a generic SplunkEvent.
     *
     */
    @Test
    public void testLogRecordToEvent() {
        Logger logger = Logger.getLogger(SplunkLogUtilTest.class.getName());
        logger.setUseParentHandlers(false);
        LogRecordCollector handler = new LogRecordCollector();
        logger.addHandler(handler);

        final String message = "test message";
        final Throwable ex = new RuntimeException("test exception");

        logger.log(Level.SEVERE, message, ex);

        assertFalse("handler did not receive any log records",
                handler.getRecords().isEmpty());

        final LogRecord lr = handler.getRecords().get(0);
        final SplunkEvent se = SplunkLogUtil.toEvent(lr);

        assertEquals(se.getEventTime(),
                ZonedDateTime.ofInstant(
                        Instant.ofEpochMilli(lr.getMillis()), ZoneOffset.UTC)
        );
        Map<String, String> fields = se.getFields();
        assertEquals(lr.getLevel().getName(),
                fields.get(prefixedFieldName(SplunkLogUtil.KEY_LEVEL)));
        assertEquals(lr.getLoggerName(),
                fields.get(prefixedFieldName(SplunkLogUtil.KEY_LOGGER)));
        assertEquals(lr.getMessage(),
                fields.get(prefixedFieldName(SplunkLogUtil.KEY_MSG)));
        assertNotNull(se.getHash());
        assertEquals(se.getHash().asHex(),
                fields.get(prefixedFieldName(SplunkLogUtil.KEY_HASH)));
        assertEquals(ex.getClass().getName(),
                fields.get(prefixedFieldName(SplunkLogUtil.KEY_EXCEPTION_CLASS)));
        assertEquals(ex.getMessage(),
                fields.get(prefixedFieldName(SplunkLogUtil.KEY_EXCEPTION_MSG)));
    }

}
