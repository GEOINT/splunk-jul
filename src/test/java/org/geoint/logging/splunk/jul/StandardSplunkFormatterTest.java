package org.geoint.logging.splunk.jul;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.geoint.logging.splunk.NativeSplunkFormatter;
import org.geoint.logging.splunk.SplunkEvent;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 */
public class StandardSplunkFormatterTest {
    
    @Test
    public void testFormat() {
        final Logger logger = Logger.getLogger(SplunkLogUtilTest.class.getName());
        logger.setUseParentHandlers(false);
        final LogRecordCollector handler = new LogRecordCollector();
        logger.addHandler(handler);
        
        final String message = "test message";
        final Throwable ex = new RuntimeException("test exception");
        
        logger.log(Level.SEVERE, message, ex);
        
        assertFalse("handler did not receive any log records",
                handler.getRecords().isEmpty());
        
        final LogRecord lr = handler.getRecords().get(0);
        final SplunkEvent se = SplunkLogUtil.toEvent(lr);
        
        StandardSplunkFormatter formatter = new StandardSplunkFormatter();
        
        final String recordFormatted = formatter.format(lr);
        final String eventFormatted
                = new NativeSplunkFormatter().format(SplunkLogUtil.toEvent(lr));
        
        System.out.println("Splunk formatted log event: ");
        System.out.println(recordFormatted);
        
        assertEquals(eventFormatted, recordFormatted);
        
    }
    
}
