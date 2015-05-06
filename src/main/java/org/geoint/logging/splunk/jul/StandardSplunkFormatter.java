package org.geoint.logging.splunk.jul;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import org.geoint.logging.splunk.FieldValueNormalizer;
import org.geoint.logging.splunk.SplunkEvent;
import org.geoint.logging.splunk.crypto.EventHash;
import org.geoint.logging.splunk.crypto.MessageDigestRecordHasher;

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


    private final List<FieldValueNormalizer> normalizers = new ArrayList<>();

    /**
     * Creates a formatter with a SHA256 record hasher.
     */
    public StandardSplunkFormatter() {
        this((raw) -> {
            //replaces double quotes with single quotes
            return raw.replace("\"", "'");
        });
    }

    public StandardSplunkFormatter(
            FieldValueNormalizer... normalizers) {
        this.normalizers.addAll(Arrays.asList(normalizers));
    }

    /**
     * Add a normalizer to the chain.
     *
     * @param normalizer
     * @return
     */
    public StandardSplunkFormatter normalizer(FieldValueNormalizer normalizer) {
        normalizers.add(normalizer);
        return this;
    }

    @Override
    public String format(LogRecord lr) {
        
    }

    private SplunkEvent asEvent(LogRecord lr) {
        if (lr instanceof SplunkEvent) {
            return (SplunkEvent) lr;
        }
        return new LogRecordEventWrapper(lr);
    }


}
