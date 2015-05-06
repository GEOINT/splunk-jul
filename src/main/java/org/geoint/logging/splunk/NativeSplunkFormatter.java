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

    @Override
    public String format(SplunkEvent event) {
        StringBuilder sb = new StringBuilder();

        sb.append(DATE_FORMATTER.format(event.getEventTime()));

        event.getFields().entrySet().stream()
                .sorted()
                .forEach((e) -> appendKV(sb, normalize(e.getKey()), e.getValue()));


        sb.append(System.lineSeparator());
        return sb.toString();
    }

    private void appendKV(StringBuilder sb, String key, String value) {
        sb.append(FIELD_SEPARATOR)
                .append(key) //normally don't need to normalize keys -- do this manually as needed
                .append(KV_SEPARATOR)
                .append(QUOTE)
                .append(normalize(value))
                .append(QUOTE);
    }

    /**
     * "Escapes" splunk field IAW their best practices.
     *
     * Escaping, for splunk, is actually substitutions.
     *
     * @param value
     */
    private String normalize(String value) {
        for (FieldValueNormalizer n : normalizers) {
            value = n.normalize(value);
        }
        return value;
    }

    private void formatException(Throwable ex, StringBuilder sb) {
        
    }

    private void appendJsonKv(StringBuilder sb, String name, String value) {
        sb.append(JSON_QUOTE)
                .append(name)
                .append(JSON_QUOTE)
                .append(JSON_KV_SEPARATOR)
                .append(JSON_QUOTE)
                .append(escapeJson(value))
                .append(JSON_QUOTE);
    }

    /**
     * Escapes the provided raw string IAW RFC 4627.
     *
     * @param raw
     * @return escaped string IAW RFC 4627
     */
    private String escapeJson(String string) {
        //shamelessly copied from Jettison v1.3.7 (Apachev2).  props!

        if (string == null || string.length() == 0) {
            return "\"\"";
        }

        char c = 0;
        int i;
        int len = string.length();
        StringBuilder sb = new StringBuilder(len + 4);
        String t;

        sb.append('"');
        for (i = 0; i < len; i += 1) {
            c = string.charAt(i);
            switch (c) {
                case '\\':
                case '"':
                    sb.append('\\');
                    sb.append(c);
                    break;
                case '/':
                    if (i > 0 && string.charAt(i - 1) == '<') {
                        sb.append('\\');
                    }
                    sb.append(c);
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                default:
                    if (c < ' ') {
                        t = "000" + Integer.toHexString(c);
                        sb.append("\\u")
                                .append(t.substring(t.length() - 4));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
