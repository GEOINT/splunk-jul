package org.geoint.logging.splunk;

/**
 * Formats a {@link SplunkEvent} as a String.
 */
@FunctionalInterface
public interface SplunkEventFormatter {

    String format(SplunkEvent event);
}
