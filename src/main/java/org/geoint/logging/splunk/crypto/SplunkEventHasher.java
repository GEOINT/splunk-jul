package org.geoint.logging.splunk.crypto;

import org.geoint.logging.splunk.SplunkEvent;

/**
 * Creates a cryptographic hash of the {@link SplunkEvent}.
 *
 */
@FunctionalInterface
public interface SplunkEventHasher {

    EventHash hash(SplunkEvent event);
}
