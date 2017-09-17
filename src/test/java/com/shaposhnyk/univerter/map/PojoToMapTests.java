package com.shaposhnyk.univerter.map;

import com.shaposhnyk.univerter.*;
import org.hamcrest.CoreMatchers;
import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;

/**
 * Converting POJO class to a map example. This is also can be used to produce a JSON from a map
 */
public class PojoToMapTests extends ConverterBase {

    private static final TriConsumer<Field, Object, Map<String, Object>> UWRITER = (f, s, ctx) -> ctx.put(f.externalName(), s);

    @Test
    public void convertAsSingleObject() {
        MySubObject subObject = null;
        MyObject input = new MyObject("Some", 42);
        Map<String, Object> ctx = new ConcurrentHashMap<>();

        Field root = Field.Factory.of("root");

        Converter<MyObject, Map<String, Object>> composer = ObjectBuilders.Factory.of(root, input, ctx)
                .field(of("name", MyObject::getName).decorateJ(String::toUpperCase))
                .field(of("myList", MyObject::getArray).mapJ((String s) -> Arrays.asList(s.split(","))))
                .field(of("myInt", MyObject::getNumberLike)
                        .mapJ(Integer::valueOf)
                        .silenceExtractionErrors()
                )
                // here we suppose that calling myObject.getSubObject() is cheap
                .field(of("subId", (MyObject o) -> o.getSubObejct().getValue()).mapJ((Integer i) -> i.toString()))
                .field(of("subName", (MyObject o) -> o.getSubObejct().getName()).decorateJ(String::toLowerCase))
                .build();

        composer.consume(input, ctx);

        Assert.assertThat(ctx.get("name"), equalTo("SOME"));
        Assert.assertThat(ctx.get("myList"), equalTo(Arrays.asList("Some1", "Some2")));
        Assert.assertThat(ctx.get("myInt"), equalTo(3));

        Assert.assertThat(ctx.get("subId"), equalTo("21"));
        Assert.assertThat(ctx.get("subName"), equalTo("ssome"));
    }

    @Test
    public void convertMultipleObjects() {
        Map<String, Object> ctx = new ConcurrentHashMap<>();

        Field fItems = Field.Factory.of("items");
        Field object = Field.Factory.of("object(unused)");

        Converter<String, Map<String, Object>> converter = ObjectBuilders.Factory.<String, Map<String, Object>>ofRoot(fItems)
                .mapCF(PojoToMapTests::createListOfMaps)
                .flatMapS(q -> findObjectByQuery(q))
                .pipeTo(
                        ObjectBuilders.Factory.<MyObject, Collection<Map<String, Object>>>ofRoot(object)
                                .mapC(PojoToMapTests::addSubMap)
                                .field(of("name", MyObject::getName).decorateJ(String::toUpperCase))
                                .field(of("myList", MyObject::getArray).mapJ((String s) -> Arrays.asList(s.split(","))))
                                .field(of("myInt", MyObject::getNumberLike)
                                        .mapJ(Integer::valueOf)
                                        .silenceExtractionErrors()
                                )
                                // here we suppose that calling myObject.getSubObject() is cheap
                                .field(of("subId", (MyObject o) -> o.getSubObejct().getValue()).mapJ((Integer i) -> i.toString()))
                                .field(of("subName", (MyObject o) -> o.getSubObejct().getName()).decorateJ(String::toLowerCase))
                                .build()
                );


        converter.consume("Some", ctx);

        List<Map<String, Object>> items = (List<Map<String, Object>>) ctx.get("items");

        Map<String, Object> ctx1 = items.get(0);
        Assert.assertThat(ctx1.get("name"), equalTo("SOME"));
        Assert.assertThat(ctx1.get("myList"), equalTo(Arrays.asList("Some1", "Some2")));
        Assert.assertThat(ctx1.get("myInt"), equalTo(3));

        Assert.assertThat(ctx1.get("subId"), equalTo("2"));
        Assert.assertThat(ctx1.get("subName"), equalTo("ssome"));

        Map<String, Object> ctx2 = items.get(1);
        Assert.assertThat(ctx2.get("name"), equalTo("SOMESOME"));
        Assert.assertThat(ctx2.get("myList"), equalTo(Arrays.asList("SomeSome1", "SomeSome2")));
        Assert.assertThat(ctx2.get("myInt"), equalTo(3));

        Assert.assertThat(ctx2.get("subId"), equalTo("4"));
        Assert.assertThat(ctx2.get("subName"), equalTo("ssomesome"));
        Assert.assertThat(items, IsCollectionWithSize.hasSize(2));
    }

    private List<MyObject> findObjectByQuery(String q) {
        return Arrays.asList(new MyObject(q, q.length()), new MyObject(q + q, 2 * q.length()));
    }

    @Test
    public void convertAsSingleObject2() {
        MySubObject subObject = null;
        MyObject input = new MyObject("Some", 42);
        Map<String, Object> ctx = new ConcurrentHashMap<>();

        Field root = Field.Factory.of("root");
        Field subObjF = Field.Factory.of("subObjF");

        Converter<MyObject, Map<String, Object>> composer = ObjectBuilders.Factory.<MyObject, Map<String, Object>>of(root)
                .field(of("name", MyObject::getName).decorateJ(String::toUpperCase))
                .field(of("myList", MyObject::getArray).mapJ((String s) -> Arrays.asList(s.split(","))))
                .field(of("myInt", MyObject::getNumberLike)
                        .mapJ(Integer::valueOf)
                        .silenceExtractionErrors()
                )
                .field(
                        // here we suppose that callingmyObject.getSubObject() is expensive
                        // so it does make sense to call it once, using simple mapping hierarchical converter
                        ObjectBuilders.Factory.of(subObjF, subObject, ctx)
                                .field(of("subId", MySubObject::getValue).mapJ((Integer i) -> i.toString()))
                                .field(of("subName", MySubObject::getName).decorateJ(String::toLowerCase))
                                .mapJS(MyObject::getSubObejct)
                )
                .build();

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
                .field(of("name", MyObject::getName).decorateJ(String::toUpperCase))
                .field(of("myList", MyObject::getArray).mapJ((String s) -> Arrays.asList(s.split(","))))
                .field(of("myInt", MyObject::getNumberLike)
                        .mapJ((String s) -> Integer.valueOf(s))
                        .silenceExtractionErrors()
                )
                .field(
                        ObjectBuilders.Factory.ofRoot(subObjF, input, ctx)
                                .mapS(MyObject::getSubObejct)
                                .mapCF(PojoToMapTests::addSubMapField)
                                .field(of("subId", MySubObject::getValue).mapJ((Integer i) -> i.toString()))
                                .field(of("subName", MySubObject::getName).decorateJ(String::toLowerCase))
                                .build()
                )
                .build();

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

    static Collection<Map<String, Object>> createListOfMaps(Field f, Map<String, Object> ctx) {
        List<Map<String, Object>> list = new ArrayList<>();
        ctx.put(f.externalName(), list);
        return list;
    }


    static Map<String, Object> addSubMap(Collection<Map<String, Object>> ctxMap) {
        Map<String, Object> map = new HashMap<>();
        ctxMap.add(map);
        return map;
    }

    static Map<String, Object> addSubMapField(Field f, Map<String, Object> ctxMap) {
        Map<String, Object> map = new HashMap<>();
        ctxMap.put(f.externalName(), map);
        return map;
    }

    <T, R> Builders.UExtracting<T, Map<String, Object>, R> of(String extName, Function<T, R> getter) {
        Field f = Field.Factory.of(extName);
        return Builders.Factory.uniExtractingOf(f, getter).withWriterJF(UWRITER);
    }
}
