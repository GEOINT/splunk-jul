package org.geoint.logging.splunk;

/**
 * Normalizes a splunk event field name or value.
 */
@FunctionalInterface
public interface SplunkNormalizer {

    /**
     * Normalizes (escape, replace, normalize) a field String.
     *
     * @param raw
     * @return
     */
    String normalize(String raw);
}
