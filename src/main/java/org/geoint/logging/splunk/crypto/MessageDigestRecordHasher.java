package org.geoint.logging.splunk.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.DatatypeConverter;
import org.geoint.logging.splunk.SplunkEvent;
import org.geoint.logging.splunk.jul.SplunkLogRecord;

/**
 * Creates a hash of the {@link SplunkLogRecord} with the provided
 * MessageDigest.
 *
 * NOT THREAD SAFE.
 */
public final class MessageDigestRecordHasher implements SplunkEventHasher {

    private final MessageDigest digest;
    //some JVM-default algorithms available
    public static final String STANDARD_MD5 = "MD5";
    public static final String STANDARD_SHA_1 = "SHA-1";
    public static final String STANDARD_SHA_256 = "SHA-256";
    private static final String FIELD_KV_GLUE = "-";

    public MessageDigestRecordHasher(MessageDigest digestor) {
        this.digest = digestor;
    }

    public MessageDigestRecordHasher(String algorithm)
            throws NoSuchAlgorithmException {
        this.digest = MessageDigest.getInstance(algorithm);
    }

    public static EventHash sha256(SplunkEvent event) {
        return usingStandardAlgorithm(STANDARD_SHA_256, event);
    }

    public static EventHash sha1(SplunkEvent event) {
        return usingStandardAlgorithm(STANDARD_SHA_1, event);
    }

    public static EventHash md5(SplunkEvent event) {
        return usingStandardAlgorithm(STANDARD_MD5, event);
    }

    private static EventHash usingStandardAlgorithm(String algorithmName,
            SplunkEvent event) {
        try {
            return new MessageDigestRecordHasher(algorithmName).hash(event);
        } catch (NoSuchAlgorithmException ex) {
            assert false : "JVM standard message digest algorithm '"
                    + algorithmName + "' was not found";
            throw new RuntimeException("Unable to generate SplunkEvent "
                    + "hash.  Expected standard JVM hash algorithm '"
                    + algorithmName + "' was not found.");
        }
    }

    @Override
    public EventHash hash(SplunkEvent event) {
        event.getFields().entrySet().stream()
                .sorted() //sort by keys natural sort order
                .map((e) -> String.join(FIELD_KV_GLUE, e.getKey(), e.getValue()))
                .forEach((kv) -> digest.update(kv.getBytes(StandardCharsets.UTF_8)));
        return new MessageDigestEventHash(digest);
    }

    private final class MessageDigestEventHash implements EventHash {

        private final String algorithmName;
        private final byte[] bytes;

        private MessageDigestEventHash(MessageDigest digest) {
            this.algorithmName = digest.getAlgorithm();
            this.bytes = digest.digest();
        }

        @Override
        public String getAlgorithmName() {
            return algorithmName;
        }

        @Override
        public String asHex() {
            return DatatypeConverter.printHexBinary(bytes);
        }

        @Override
        public byte[] asBytes() {
            return bytes;
        }

    }
}
