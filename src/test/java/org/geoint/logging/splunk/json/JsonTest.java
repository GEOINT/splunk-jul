package org.geoint.logging.splunk.json;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 */
public class JsonTest {

    @Test
    public void testCreateEmptyJsonObject() {
        assertEquals("{}", Json.newObject().toString());
    }

    @Test
    public void testCreateEmptyJsonArray() {
        assertEquals("[{}]", Json.newArray().toString());
    }

    @Test
    public void testCreateStringElement() {
        final Json test = Json.newObject();
        test.element("foo", "bar");
        assertEquals("{\"foo\":\"bar\"}", test.toString());
    }

    @Test
    public void testCreateObject() {
        final Json test = Json.newObject();
        test.object("foo");
        test.element("bar", "baz");
        assertEquals("{\"foo\":{\"bar\":\"baz\"}}", test.toString());
    }

    @Test
    public void testCreateArray() {
        final Json test = Json.newArray();
        test.object("foo");
        test.element("one", "oneValue");
        test.close();
        test.nextArrayElement();
        test.object("bar");
        test.element("two", "twoValue");
        assertEquals("[{\"foo\":{\"one\":\"oneValue\"}},"
                + "{\"bar\":{\"two\":\"twoValue\"}}]",
                test.toString());
    }

    /**
     * Test creating a complex JSON hierarchy.
     */
    @Test
    public void testCreateHierarchicalJson() {

        final Json test = Json.newObject();
        test.element("first", "element");
        test.object("objectOne");
        test.element("objectOneElementOne", "blah");
        test.close();
        test.element("second", "element");
        assertEquals("{\"first\":\"element\","
                + "\"objectOne\":{\"objectOneElementOne\":\"blah\"},"
                + "\"second\":\"element\"}",
                test.toString());
    }

    /**
     * Test creating a Json array from method reference API
     */
    @Test
    public void testAsArray() {
        final MockSimpleObject obj1 = MockSimpleObject.generate();
        final MockSimpleObject obj2 = MockSimpleObject.generate();

        final Json test = Json.asArray(
                (json, obj) -> {
                    json.element("mockValue", obj.getValue());
                },
                obj1, obj2
        );

        assertEquals("[{\"mockValue\":\""+obj1.getValue()+"\"}"
                + ",{\"mockValue\":\""+obj2.getValue()+"\"}]",
                test.toString());
    }

    private static class MockSimpleObject {

        private final double value1;

        public MockSimpleObject(double value1) {
            this.value1 = value1;
        }

        public static MockSimpleObject generate() {
            return new MockSimpleObject(Math.random());
        }

        public double getValue() {
            return value1;
        }

    }
}
