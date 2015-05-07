
package org.geoint.logging.splunk.json;

/**
 * Creates a JSON object for the provided object.
 * 
 * @param <T> object type
 */
@FunctionalInterface
public interface JsonObjectConverter<T> {

    /**
     * Convert the provided object to a Json object using the provided Json 
     * instance.
     * 
     * @param json
     * @param object 
     */
    void convert(Json json, T object);
    
}
