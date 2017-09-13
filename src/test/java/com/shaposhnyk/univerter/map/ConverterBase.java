package com.shaposhnyk.univerter.map;

import com.shaposhnyk.univerter.Builders;
import com.shaposhnyk.univerter.Converter;
import com.shaposhnyk.univerter.Field;
import org.hamcrest.Matcher;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test base class
 */
public class ConverterBase {

    public Field fInt() {
        return Field.Factory.of("int", "ext");
    }

    public Field fName() {
        return Field.Factory.of("name", "displayName");
    }

    public Builders.Simple<MyObject, Map<String, Object>> simpleInt() {
        final Field f = fInt();
        return Builders.Factory.simpleOf(f).withJConsumer((s, c) -> c.put(f.externalName(), s.getName()));
    }

    public Map<String, Object> assertConvertionOnSome(Converter<MyObject, Map<String, Object>> conv, Matcher<Object> valueMatcher) {
        MyObject source1 = new MyObject("Some", 42);
        Map<String, Object> work = new ConcurrentHashMap<>(); // disallow nulls
        conv.consume(source1, work);

        assertThat(work.values(), everyItem(valueMatcher));
        assertThat(work.keySet(), everyItem(equalTo(conv.externalName())));
        return work;
    }

    public Map<String, Object> assertNoConvertionOnNull(Converter<MyObject, Map<String, Object>> conv) {
        MyObject source1 = new MyObject(null, 42);
        Map<String, Object> work = new ConcurrentHashMap<>(); // disallow nulls
        conv.consume(source1, work);

        assertThat(work, equalTo(Collections.emptyMap()));
        return work;
    }
}