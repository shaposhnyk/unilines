package com.shaposhnyk.univerter.map;

import com.shaposhnyk.univerter.*;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;

/**
 * Convertion to map tests. This is also useful to output JSONs
 */
public class MapTests extends ConverterBase {

    private static final TriConsumer<Field, Object, Map<String, Object>> UWRITER = (f, s, ctx) -> ctx.put(f.externalName(), s);

    @Test
    public void convertSingleObject() {
        MySubObject subObject = null;
        MyObject input = new MyObject("Some", 42);
        Map<String, Object> ctxType = new ConcurrentHashMap<>();

        Field root = Field.Factory.of("root");

        Converter<MyObject, Map<String, Object>> composer = ObjectBuilders.Factory.of(root, input, ctxType)
                .field(of("name", MyObject::getName).withJDecorator(String::toUpperCase))
                .field(of("myList", MyObject::getArray).withJTransformer((String s) -> Arrays.asList(s.split(","))))
                .field(of("myInt", MyObject::getNumberLike)
                        .withJTransformer(Integer::valueOf)
                        .ignoreErrors()
                ).composer();

        composer.consume(input, ctxType);

        Assert.assertThat(ctxType.get("name"), equalTo("SOME"));
        Assert.assertThat(ctxType.get("myList"), equalTo(Arrays.asList("Some1", "Some2")));
        Assert.assertThat(ctxType.get("myInt"), equalTo(3));
    }


    @Test
    public void convertFromSubObject() {
        MySubObject subObject = null;
        MyObject input = new MyObject("Some", 42);
        Map<String, Object> ctx = new ConcurrentHashMap<>();

        Field root = Field.Factory.of("root");
        Field subObjF = Field.Factory.of("subObjF");

        Converter<MyObject, Map<String, Object>> composer = ObjectBuilders.Factory.of(root, input, ctx)
                .field(of("name", MyObject::getName).withJDecorator(String::toUpperCase))
                .field(of("myList", MyObject::getArray).withJTransformer((String s) -> Arrays.asList(s.split(","))))
                .field(of("myInt", MyObject::getNumberLike)
                        .withJTransformer(Integer::valueOf)
                        .ignoreErrors()
                )
                .field(
                        ObjectBuilders.Factory.of(subObjF, subObject, ctx)
                                .field(of("subId", MySubObject::getValue).withJTransformer((Integer i) -> i.toString()))
                                .field(of("subName", MySubObject::getName).withJDecorator(String::toLowerCase))
                                .composer(MyObject::getSubObejct)
                )
                .composer();

        composer.consume(input, ctx);

        Assert.assertThat(ctx.get("name"), equalTo("SOME"));
        Assert.assertThat(ctx.get("myList"), equalTo(Arrays.asList("Some1", "Some2")));
        Assert.assertThat(ctx.get("myInt"), equalTo(3));

        Assert.assertThat(ctx.get("subId"), equalTo("21"));
        Assert.assertThat(ctx.get("subName"), equalTo("ssome"));
    }

    @Test
    public void convertOnSubObject() {
        MySubObject subObject = null;
        MyObject input = new MyObject("Some", 42);
        Map<String, Object> ctx = new ConcurrentHashMap<>();

        Field root = Field.Factory.of("root");
        Field subObjF = Field.Factory.of("myObj");

        Converter<MyObject, Map<String, Object>> composer = ObjectBuilders.Factory.of(root, input, ctx)
                .field(of("name", MyObject::getName).withJDecorator(String::toUpperCase))
                .field(of("myList", MyObject::getArray).withJTransformer((String s) -> Arrays.asList(s.split(","))))
                .field(of("myInt", MyObject::getNumberLike)
                        .withJTransformer((String s) -> Integer.valueOf(s))
                        .ignoreErrors()
                )
                .field(
                        ObjectBuilders.Factory.of(subObjF, subObject, ctx)
                                .field(of("subId", MySubObject::getValue).withJTransformer((Integer i) -> i.toString()))
                                .field(of("subName", MySubObject::getName).withJDecorator(String::toLowerCase))
                                .composer(MyObject::getSubObejct)
                                .decorateJFContext(this::createSubMap)
                )
                .composer();

        composer.consume(input, ctx);

        Assert.assertThat(ctx.get("name"), equalTo("SOME"));
        Assert.assertThat(ctx.get("myList"), equalTo(Arrays.asList("Some1", "Some2")));
        Assert.assertThat(ctx.get("myInt"), equalTo(3));

        Map<String, Object> myObj = (Map<String, Object>) ctx.get("myObj");
        Assert.assertThat(myObj, not(equalTo(Collections.emptyMap())));
        Assert.assertThat(myObj.keySet(), CoreMatchers.hasItems("subId", "subName"));
        Assert.assertThat(myObj.get("subId"), equalTo("21"));
        Assert.assertThat(myObj.get("subName"), equalTo("ssome"));
    }

    Map<String, Object> createSubMap(Field f, Map<String, Object> ctxMap) {
        Map<String, Object> map = new HashMap<>();
        ctxMap.put(f.externalName(), map);
        return map;
    }

    <T, R> Builders.UExtracting<T, Map<String, Object>, R> of(String extName, Function<T, R> getter) {
        Field f = Field.Factory.of(extName);
        return Builders.Factory.uniExtractingOf(f, getter).withJFWriter(UWRITER);
    }
}
