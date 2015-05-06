
package org.geoint.logging.splunk.crypto;

/**
 * Hash of a SplunkEvent.
 */
public interface EventHash {

    String getAlgorithmName();
    
    String asHex();
    
    byte[] asBytes();
    
}
