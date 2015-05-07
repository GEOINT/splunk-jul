package org.geoint.logging.splunk.jul;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import org.geoint.logging.splunk.NativeSplunkFormatter;
import org.geoint.logging.splunk.SplunkEvent;

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

    public static final StandardSplunkFormatter DEFAULT
            = new StandardSplunkFormatter();

    @Override
    public String format(LogRecord lr) {
        return format(asEvent(lr));
    }

    private String format(SplunkEvent event) {
        return new NativeSplunkFormatter().format(event);
    }

    private SplunkEvent asEvent(LogRecord lr) {
        return SplunkLogUtil.toEvent(lr);
    }

}
