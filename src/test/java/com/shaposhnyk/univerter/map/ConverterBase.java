package com.shaposhnyk.univerter.map;

import com.shaposhnyk.univerter.UBiPipeline;
import com.shaposhnyk.univerter.UField;
import com.shaposhnyk.univerter.builders.UCField;
import com.shaposhnyk.univerter.map.helpers.MyObject;
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

    public UField fInt() {
        return UField.Factory.of("int", "ext");
    }

    public UField fName() {
        return UField.Factory.of("name", "displayName");
    }

    public UCField.Simple<MyObject, Map<String, Object>> simpleInt() {
        final UField f = fInt();
        return UCField.Builder.of(f).withConsumerJ((s, c) -> c.put(f.externalName(), s.getName()));
    }

    public Map<String, Object> assertConvertionOnSome(UBiPipeline<MyObject, Map<String, Object>> conv, Matcher<Object> valueMatcher) {
        MyObject source1 = new MyObject("Some", 42);
        Map<String, Object> work = new ConcurrentHashMap<>(); // disallow nulls
        conv.consume(source1, work);

        assertThat(work.values(), everyItem(valueMatcher));
        assertThat(work.keySet(), everyItem(equalTo(conv.externalName())));
        return work;
    }

    public Map<String, Object> assertNoConvertionOnNull(UBiPipeline<MyObject, Map<String, Object>> conv) {
        MyObject source1 = new MyObject(null, 42);
        Map<String, Object> work = new ConcurrentHashMap<>(); // disallow nulls
        conv.consume(source1, work);

        assertThat(work, equalTo(Collections.emptyMap()));
        return work;
    }
}
