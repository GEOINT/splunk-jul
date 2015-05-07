package org.geoint.logging.splunk.json;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

/*
 * yes, there are 1k JSON libraries for Java, but that would require 
 * dependencies, which I want to avoid
 */
/**
 * Creates a JSON string programmatically.
 *
 * This implementation currently isn't "bullet proof" and does allow the user to
 * create invalid JSON if called out of sequence.
 *
 *
 * Json instances are not thread-safe.
 *
 *
 * TODO add checks/exceptions if user attempts to create invalid JSON.
 *
 * TODO add read/parsing capabilities (with invalid JSON detection).
 */
public class Json implements Serializable, CharSequence, Comparable<Json> {

    private static enum JsonContainer {

        OBJECT(JSON_OBJ_WRAPPER, 1), ARRAY(JSON_ARRAY_WRAPPER, 2);
        //container wrapper 
        private final String wrapper;
        //how much to decrease the position by for this container wrapper
        private final int positionOffset;

        private JsonContainer(String wrapper, int positionOffset) {
            this.wrapper = wrapper;
            this.positionOffset = positionOffset;
        }

        public String wrapper() {
            return wrapper;
        }

        public int offset() {
            return positionOffset;
        }
    };

    private final StringBuilder json;
    private int position;
    private JsonContainer container = JsonContainer.OBJECT;
    //indicates if this is the first element in the current container
    private boolean containerFirst;

    private static final String JSON_OBJ_WRAPPER = "{}";
    private static final String JSON_ARRAY_WRAPPER = "[{}]";
    private static final String JSON_KV_SEPARATOR = ":";
    private static final String JSON_ELEMENT_SEPARATOR = ",";

    private Json(JsonContainer containerType) {
        this.containerFirst = true;
        this.json = new StringBuilder(containerType.wrapper());
        this.position
                = containerType.wrapper().length() - containerType.offset();
    }

    public static Json newObject() {
        return new Json(JsonContainer.OBJECT);
    }

    public static Json newArray() {
        return new Json(JsonContainer.ARRAY);
    }

    /**
     * Creates a new JSON array containing the provided objects as elements
     * using the {@link JsonObjectConverter} to serialize each object.
     *
     * @param <T>
     * @param converter
     * @param objects
     * @return JSON array of the objects
     */
    public static <T> Json asArray(JsonObjectConverter<T> converter,
            T... objects) {

        final Json json = Json.newArray();

        if (objects == null || objects.length == 0) {
            return json;
        }

        Iterator<T> iterator = Arrays.asList(objects).iterator();
        while (iterator.hasNext()) {
            T obj = iterator.next();
            converter.convert(json, obj);

            if (iterator.hasNext()) {
                json.nextArrayElement();
            }
        }
        return json;
    }

    /**
     * Create a new object element.
     *
     * @param name
     * @return fluid interface
     */
    public Json object(String name) {
        container(name, JsonContainer.OBJECT);
        return this;
    }

    /**
     *
     * @param name
     * @param value
     * @return fluid interface
     */
    public Json element(String name, String value) {
        appendJson(escape(name),
                JSON_KV_SEPARATOR,
                escape(value)); //no need to check null, escape does this
        return this;
    }

    public Json element(String name, double value) {
        return element(name, String.valueOf(value));
    }

    public Json element(String name, int value) {
        return element(name, String.valueOf(value));
    }

    public Json element(String name, boolean bool) {
        return element(name, String.valueOf(bool));
    }

    public Json element(String name, float value) {
        return element(name, String.valueOf(value));
    }

    public Json element(String name, long value) {
        return element(name, String.valueOf(value));
    }

    public Json element(String name, short value) {
        return element(name, String.valueOf(value));
    }

    public Json element(String name, Object value) {
        return element(name, (value == null) ? "" : String.valueOf(value));
    }

    /**
     * Create a new array element.
     *
     * @param name
     * @return
     */
    public Json array(String name) {
        appendJson(escape(name),
                JSON_KV_SEPARATOR,
                JSON_ARRAY_WRAPPER);
        container = JsonContainer.OBJECT;
        position--; //move position inside the array
        return this;
    }

    /**
     * Advance to the next array element, if the current position is in a JSON
     * array.
     *
     * @return fluid interface
     */
    public Json nextArrayElement() {
        if (container.equals(JsonContainer.OBJECT)) {
            //array object
            close();
        }

        if (!container.equals(JsonContainer.ARRAY)) {
            //TODO better error handling
            throw new RuntimeException("Expected Json array container.");
        }

        //create a new array object (element)
        appendJson(JSON_OBJ_WRAPPER);
        position--;
        containerFirst = true;

        return this;
    }

    /**
     * Close the current JSON structural container (object, array), moving the
     * position up the JSON structural hierarchy.
     *
     * This method does not need to be called to close the encapsulating JSON
     * array or object before calling #toString, but no harm is done if it is.
     *
     * @return fluid interface
     */
    public Json close() {

        //don't "close" the root JSON wrapping (array or object) because it just 
        //introduces more state handling requirements
        if (json.length() - position == 0) {
            return this;
        }

        //"close" by moving forward past the current container closing
        position++;
//        containerFirst = false;

        //what kind of container is this currently?
        if (json.charAt(position) == '}') {
            container = JsonContainer.OBJECT;
        } else if (charAt(position) == ']') {
            container = JsonContainer.ARRAY;
        } else {
            throw new RuntimeException("Unexpected JSON state");
        }

        return this;
    }

    @Override
    public int length() {
        return json.length();
    }

    @Override
    public char charAt(int index) {
        return json.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return json.subSequence(start, end);
    }

    @Override
    public int compareTo(Json o) {
        return this.json.toString().compareTo(o.json.toString());
    }

    /**
     *
     * @return as RFC 4627 compliant JSON formatted string
     */
    @Override
    public String toString() {
        return json.toString();
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 59 * hash + Objects.hashCode(this.json);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Json other = (Json) obj;
        if (!Objects.equals(this.json, other.json)) {
            return false;
        }
        return true;
    }

    /**
     * appends the container context and positions correctly.
     *
     * @param containerName
     * @param container
     */
    private void container(String containerName, JsonContainer container) {
        appendJson(escape(containerName),
                JSON_KV_SEPARATOR,
                container.wrapper());
        position -= container.offset(); //move position inside of the object
        containerFirst = true;
    }

    /**
     * Appends a "complete" (valid) snippit of JSON, ensuring the resultant JSON
     * is still properly formatted and the position is updated.
     *
     * @param jsonSnippit
     */
    private void appendJson(String... jsonSnippits) {
        if (!containerFirst) {
            json.insert(position, JSON_ELEMENT_SEPARATOR);
            position++;
        }

        containerFirst = false;

        Arrays.stream(jsonSnippits) //do not change to parallel or order/sort
                .map((s) -> {
                    json.insert(position, s); //insert at position (not append)
                    return s.length();
                })
                .forEach((l) -> position += l); //increment position
    }

    /**
     * Escapes the provided raw string IAW RFC 4627.
     *
     * @param raw
     * @return escaped string IAW RFC 4627
     */
    private String escape(String string) {
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
