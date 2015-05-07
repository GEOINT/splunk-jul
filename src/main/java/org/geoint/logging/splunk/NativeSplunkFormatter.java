package org.geoint.logging.splunk;

import java.time.format.DateTimeFormatter;

/**
 * Formats a {@link SplunkEvent} as a String which is natively readable by
 * splunk.
 */
public class NativeSplunkFormatter implements SplunkEventFormatter {
    
    private static final String DATE_FORMAT = "yyyy-MM-dd hh:mm:ss.SSS Z";
    private static final DateTimeFormatter DATE_FORMATTER
            = DateTimeFormatter.ofPattern(DATE_FORMAT);
    private static final char KV_SEPARATOR = '=';
    private static final String FIELD_SEPARATOR = ", ";
    private static final char QUOTE = '"';
    
    @Override
    public String format(SplunkEvent event) {
        StringBuilder sb = new StringBuilder();
        
        sb.append(DATE_FORMATTER.format(event.getEventTime()));
        
        event.getFields().entrySet().stream()
                .sorted((o1, o2) -> o1.getKey().compareTo(o2.getKey()))
                .forEach((e) -> appendKV(sb, escape(e.getKey()), e.getValue()));
        
        sb.append(System.lineSeparator());
        return sb.toString();
    }
    
    private void appendKV(StringBuilder sb, String key, String value) {
        sb.append(FIELD_SEPARATOR)
                .append(key) //normally don't need to normalize keys -- do this manually as needed
                .append(KV_SEPARATOR)
                .append(QUOTE)
                .append(escape(value))
                .append(QUOTE);
    }

    /**
     * "Escapes" splunk field IAW their best practices.
     *
     * Escaping, for splunk, is actually substitutions.
     *
     * @param value
     */
    private String escape(String value) {
        return value.replace("\"", "'");
    }
    
}
