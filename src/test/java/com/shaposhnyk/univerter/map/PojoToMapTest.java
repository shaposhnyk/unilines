package com.shaposhnyk.univerter.map;

import com.shaposhnyk.univerter.UBiPipeline;
import com.shaposhnyk.univerter.UField;
import com.shaposhnyk.univerter.UTriConsumer;
import com.shaposhnyk.univerter.builders.UCField;
import com.shaposhnyk.univerter.builders.UCObjects;
import com.shaposhnyk.univerter.map.helpers.MyObject;
import com.shaposhnyk.univerter.map.helpers.MySubObject;
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
 * Converting from POJO objects to a map example. This is also can be used to produce a JSON from a map
 */
public class PojoToMapTest extends ConverterBase {

    private static final UTriConsumer<UField, Object, Map<String, Object>> UWRITER = (f, s, ctx) -> ctx.put(f.externalName(), s);

    @Test
    public void convertAsSingleObject() {
        MyObject input = new MyObject("Some", 42);
        Map<String, Object> ctx = new ConcurrentHashMap<>(); // to throw an exception on nulls

        UField root = UField.Factory.of("root");
        UBiPipeline<MyObject, Map<String, Object>> composer = UCObjects.Builder.of(root)
                .ofSourceType(MyObject.class)
                .ofContextType(ctx)
                .field(of("name", MyObject::getName).decorate(String::toUpperCase))
                .field(of("myList", MyObject::getArray).map((String s) -> Arrays.asList(s.split(","))))
                .field(of("myInt", MyObject::getNumberLike)
                        .map(Integer::valueOf)
                        .silenceExtractionErrors()
                )
                // here we suppose that calling myObject.getSubObject() is cheap
                .field(of("subId", (MyObject o) -> o.getSubObject().getValue()).mapJ((Integer i) -> i.toString()))
                .field(of("subName", (MyObject o) -> o.getSubObject().getName()).decorateJ(String::toLowerCase))
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
        UBiPipeline<String, Map<String, Object>> converter = iteratingConverter();

        Map<String, Object> ctx = new ConcurrentHashMap<>();
        converter.consume("Some", ctx);

        List<Map<String, Object>> items = (List<Map<String, Object>>) ctx.get("items");

        // first object produced by findObjectsByQuery
        Map<String, Object> ctx1 = items.get(0);
        Assert.assertThat(ctx1.get("name"), equalTo("SOME"));
        Assert.assertThat(ctx1.get("myList"), equalTo(Arrays.asList("Some1", "Some2")));
        Assert.assertThat(ctx1.get("myInt"), equalTo(3));

        Assert.assertThat(ctx1.get("subId"), equalTo("2"));
        Assert.assertThat(ctx1.get("subName"), equalTo("ssome"));

        // second object produced by findObjectsByQuery
        Map<String, Object> ctx2 = items.get(1);
        Assert.assertThat(ctx2.get("name"), equalTo("SOMESOME"));
        Assert.assertThat(ctx2.get("myList"), equalTo(Arrays.asList("SomeSome1", "SomeSome2")));
        Assert.assertThat(ctx2.get("myInt"), equalTo(3));

        Assert.assertThat(ctx2.get("subId"), equalTo("4"));
        Assert.assertThat(ctx2.get("subName"), equalTo("ssomesome"));

        // two objects at all
        Assert.assertThat(items, IsCollectionWithSize.hasSize(2));
    }

    @Test
    public void iterConverterStructureIsPreserved() {
        UBiPipeline<String, Map<String, Object>> converter = iteratingConverter();

        Assert.assertThat(converter.externalName(), equalTo("items"));

        UBiPipeline<?, ?> objConv = converter.fields().get(0);
        Assert.assertThat(objConv.externalName(), equalTo("object(docOnly)"));
        Assert.assertThat(converter.fields(), IsCollectionWithSize.hasSize(1));

        Assert.assertThat(objConv.fields(), IsCollectionWithSize.hasSize(5));

        Assert.assertThat(objConv.fields().get(0).externalName(), equalTo("name"));
        Assert.assertThat(objConv.fields().get(1).externalName(), equalTo("myList"));
        Assert.assertThat(objConv.fields().get(3).externalName(), equalTo("subId"));
    }

    @Test
    public void nestedObjectConverterStructureIsPreserved() {
        UBiPipeline<MyObject, Map<String, Object>> objConv = nesteObjectConverter();

        Assert.assertThat(objConv.externalName(), equalTo("root"));

        List<UBiPipeline<?, ?>> fields = objConv.fields();
        Assert.assertThat(fields.get(0).externalName(), equalTo("name"));
        Assert.assertThat(fields.get(1).externalName(), equalTo("myList"));
        Assert.assertThat(fields.get(3).externalName(), equalTo("myObj"));

        Assert.assertThat(fields, IsCollectionWithSize.hasSize(4));

        List<UBiPipeline<?, ?>> subFields = fields.get(3).fields();

        Assert.assertThat(subFields.get(0).externalName(), equalTo("subId"));
        Assert.assertThat(subFields.get(1).externalName(), equalTo("subName"));

        Assert.assertThat(subFields, IsCollectionWithSize.hasSize(2));
    }

    @Test
    public void objectPipeToIsSameAsBuild() {
        UField root = UField.Factory.of("root");
        Map<String, Object> ctx = new ConcurrentHashMap<>();

        UBiPipeline<MyObject, Map<String, Object>> fieldsConv = UCObjects.Builder.of(root)
                .ofSourceType(MyObject.class)
                .ofContextType(ctx)
                .field(of("name", MyObject::getName).decorateJ(String::toUpperCase))
                .build();

        UBiPipeline<MyObject, Map<String, Object>> pipeToConv = UCObjects.Builder.of(root)
                .ofSourceType(MyObject.class)
                .ofContextType(ctx)
                .pipeTo(of("name", MyObject::getName).decorateJ(String::toUpperCase));

        Assert.assertThat(fieldsConv.externalName(), equalTo("root"));
        Assert.assertThat(pipeToConv.externalName(), equalTo("root"));

        Assert.assertThat(fieldsConv.fields().get(0).externalName(), equalTo("name"));
        Assert.assertThat(pipeToConv.fields().get(0).externalName(), equalTo("name"));


        Assert.assertThat(fieldsConv.fields(), IsCollectionWithSize.hasSize(1));
        Assert.assertThat(pipeToConv.fields(), IsCollectionWithSize.hasSize(1));
    }

    private UBiPipeline<String, Map<String, Object>> iteratingConverter() {
        UField fItems = UField.Factory.of("items");
        UField fObject = UField.Factory.of("object(docOnly)");

        return UCObjects.Builder.of(fItems)
                .ofSourceType(String.class)
                .ofContextMapF(PojoToMapTest::newListOfMaps)
                .iterateOn(q -> findObjectsByQuery(q))
                .pipeTo(
                        UCObjects.Builder.of(fObject)
                                .ofSourceType(MyObject.class)
                                .ofContextMap(PojoToMapTest::addSubMap)
                                .field(of("name", MyObject::getName).decorateJ(String::toUpperCase))
                                .field(of("myList", MyObject::getArray).mapJ((String s) -> Arrays.asList(s.split(","))))
                                .field(of("myInt", MyObject::getNumberLike)
                                        .mapJ(Integer::valueOf)
                                        .silenceExtractionErrors()
                                )
                                // here we suppose that calling myObject.getSubObject() is cheap
                                .field(of("subId", (MyObject o) -> o.getSubObject().getValue()).mapJ((Integer i) -> i.toString()))
                                .field(of("subName", (MyObject o) -> o.getSubObject().getName()).decorateJ(String::toLowerCase))
                                .build()
                );
    }

    @Test
    public void convertAsSingleObject2() {
        MySubObject subObject = null;
        MyObject input = new MyObject("Some", 42);
        Map<String, Object> ctx = new ConcurrentHashMap<>();

        UField root = UField.Factory.of("root");
        UField subObjF = UField.Factory.of("subObjF");

        UBiPipeline<MyObject, Map<String, Object>> composer = UCObjects.Builder.of(root)
                .ofSourceType(MyObject.class)
                .ofContextType(ctx)
                .field(of("name", MyObject::getName).decorateJ(String::toUpperCase))
                .field(of("myList", MyObject::getArray).mapJ((String s) -> Arrays.asList(s.split(","))))
                .field(of("myInt", MyObject::getNumberLike)
                        .mapJ(Integer::valueOf)
                        .silenceExtractionErrors()
                )
                .field(
                        // here we suppose that calling myObject.getSubObject() is expensive
                        // so it does make sense to call it once, using simple mapping hierarchical converter
                        // however, note, that we are still writing the output to the same map
                        UCObjects.Builder.of(subObjF)
                                .ofSourceMap(MyObject::getSubObject)
                                .ofContextType(ctx)
                                .field(of("subId", MySubObject::getValue).mapJ((Integer i) -> i.toString()))
                                .field(of("subName", MySubObject::getName).decorateJ(String::toLowerCase))
                                .build()
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
        MyObject input = new MyObject("Some", 42);
        Map<String, Object> ctx = new ConcurrentHashMap<>();

        UBiPipeline<MyObject, Map<String, Object>> composer = nesteObjectConverter();

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

    private UBiPipeline<MyObject, Map<String, Object>> nesteObjectConverter() {
        UField root = UField.Factory.of("root");
        UField subObjF = UField.Factory.of("myObj");
        Map<String, Object> ctx = new ConcurrentHashMap<>();

        return UCObjects.Builder.of(root)
                .ofSourceType(MyObject.class)
                .ofContextType(ctx)
                .field(of("name", MyObject::getName).decorateJ(String::toUpperCase))
                .field(of("myList", MyObject::getArray).mapJ((String s) -> Arrays.asList(s.split(","))))
                .field(of("myInt", MyObject::getNumberLike)
                        .mapJ((String s) -> Integer.valueOf(s))
                        .silenceExtractionErrors()
                )
                .field(
                        UCObjects.Builder.of(subObjF)
                                .ofSourceMap(MyObject::getSubObject)
                                .ofContextMapF(PojoToMapTest::addSubMapField)
                                .field(of("subId", MySubObject::getValue).mapJ((Integer i) -> i.toString()))
                                .field(of("subName", MySubObject::getName).decorateJ(String::toLowerCase))
                                .build()
                )
                .build();
    }

    private List<MyObject> findObjectsByQuery(String q) {
        return Arrays.asList(new MyObject(q, q.length()), new MyObject(q + q, 2 * q.length()));
    }

    static Collection<Map<String, Object>> newListOfMaps(UField f, Map<String, Object> ctx) {
        List<Map<String, Object>> list = new ArrayList<>();
        ctx.put(f.externalName(), list);
        return list;
    }


    static Map<String, Object> addSubMap(Collection<Map<String, Object>> ctxMap) {
        Map<String, Object> map = new HashMap<>();
        ctxMap.add(map);
        return map;
    }

    static Map<String, Object> addSubMapField(UField f, Map<String, Object> ctxMap) {
        Map<String, Object> map = new HashMap<>();
        ctxMap.put(f.externalName(), map);
        return map;
    }

    <T, R> UCField.UExtracting<T, Map<String, Object>, R> of(String extName, Function<T, R> getter) {
        UField f = UField.Factory.of(extName);
        return UCField.Builder.uniExtractingOf(f, getter).withWriterJF(UWRITER);
    }
}
