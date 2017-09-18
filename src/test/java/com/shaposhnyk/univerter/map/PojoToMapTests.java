package com.shaposhnyk.univerter.map;

import com.shaposhnyk.univerter.Converter;
import com.shaposhnyk.univerter.Field;
import com.shaposhnyk.univerter.TriConsumer;
import com.shaposhnyk.univerter.builders.Objects;
import com.shaposhnyk.univerter.builders.Simples;
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
public class PojoToMapTests extends ConverterBase {

    private static final TriConsumer<Field, Object, Map<String, Object>> UWRITER = (f, s, ctx) -> ctx.put(f.externalName(), s);

    @Test
    public void convertAsSingleObject() {
        MyObject input = new MyObject("Some", 42);
        Map<String, Object> ctx = new ConcurrentHashMap<>(); // to throw an exception on nulls

        Field root = Field.Factory.of("root");
        Converter<MyObject, Map<String, Object>> composer = Objects.Builder.ofField(root)
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
        Converter<String, Map<String, Object>> converter = iteratingConverter();

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
        Converter<String, Map<String, Object>> converter = iteratingConverter();

        Assert.assertThat(converter.externalName(), equalTo("items"));

        Converter<?, ?> objConv = converter.fields().get(0);
        Assert.assertThat(objConv.externalName(), equalTo("object(docOnly)"));
        Assert.assertThat(converter.fields(), IsCollectionWithSize.hasSize(1));

        Assert.assertThat(objConv.fields(), IsCollectionWithSize.hasSize(5));

        Assert.assertThat(objConv.fields().get(0).externalName(), equalTo("name"));
        Assert.assertThat(objConv.fields().get(1).externalName(), equalTo("myList"));
        Assert.assertThat(objConv.fields().get(3).externalName(), equalTo("subId"));
    }

    @Test
    public void nestedObjectConverterStructureIsPreserved() {
        Converter<MyObject, Map<String, Object>> objConv = nesteObjectConverter();

        Assert.assertThat(objConv.externalName(), equalTo("root"));

        List<Converter<?, ?>> fields = objConv.fields();
        Assert.assertThat(fields.get(0).externalName(), equalTo("name"));
        Assert.assertThat(fields.get(1).externalName(), equalTo("myList"));
        Assert.assertThat(fields.get(3).externalName(), equalTo("myObj"));

        Assert.assertThat(fields, IsCollectionWithSize.hasSize(4));

        List<Converter<?, ?>> subFields = fields.get(3).fields();

        Assert.assertThat(subFields.get(0).externalName(), equalTo("subId"));
        Assert.assertThat(subFields.get(1).externalName(), equalTo("subName"));

        Assert.assertThat(subFields, IsCollectionWithSize.hasSize(2));
    }

    @Test
    public void objectPipeToIsSameAsBuild() {
        Field root = Field.Factory.of("root");
        Map<String, Object> ctx = new ConcurrentHashMap<>();

        Converter<MyObject, Map<String, Object>> fieldsConv = Objects.Builder.ofField(root)
                .ofSourceType(MyObject.class)
                .ofContextType(ctx)
                .field(of("name", MyObject::getName).decorateJ(String::toUpperCase))
                .build();

        Converter<MyObject, Map<String, Object>> pipeToConv = Objects.Builder.ofField(root)
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

    private Converter<String, Map<String, Object>> iteratingConverter() {
        Field fItems = Field.Factory.of("items");
        Field fObject = Field.Factory.of("object(docOnly)");

        return Objects.Builder.ofField(fItems)
                .ofSourceType(String.class)
                .ofContextMapF(PojoToMapTests::newListOfMaps)
                .iterateOn(q -> findObjectsByQuery(q))
                .pipeTo(
                        Objects.Builder.ofField(fObject)
                                .ofSourceType(MyObject.class)
                                .ofContextMap(PojoToMapTests::addSubMap)
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

        Field root = Field.Factory.of("root");
        Field subObjF = Field.Factory.of("subObjF");

        Converter<MyObject, Map<String, Object>> composer = Objects.Builder.ofField(root)
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
                        Objects.Builder.ofField(subObjF)
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

        Converter<MyObject, Map<String, Object>> composer = nesteObjectConverter();

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

    private Converter<MyObject, Map<String, Object>> nesteObjectConverter() {
        Field root = Field.Factory.of("root");
        Field subObjF = Field.Factory.of("myObj");
        Map<String, Object> ctx = new ConcurrentHashMap<>();

        return Objects.Builder.ofField(root)
                .ofSourceType(MyObject.class)
                .ofContextType(ctx)
                .field(of("name", MyObject::getName).decorateJ(String::toUpperCase))
                .field(of("myList", MyObject::getArray).mapJ((String s) -> Arrays.asList(s.split(","))))
                .field(of("myInt", MyObject::getNumberLike)
                        .mapJ((String s) -> Integer.valueOf(s))
                        .silenceExtractionErrors()
                )
                .field(
                        Objects.Builder.ofField(subObjF)
                                .ofSourceMap(MyObject::getSubObject)
                                .ofContextMapF(PojoToMapTests::addSubMapField)
                                .field(of("subId", MySubObject::getValue).mapJ((Integer i) -> i.toString()))
                                .field(of("subName", MySubObject::getName).decorateJ(String::toLowerCase))
                                .build()
                )
                .build();
    }

    private List<MyObject> findObjectsByQuery(String q) {
        return Arrays.asList(new MyObject(q, q.length()), new MyObject(q + q, 2 * q.length()));
    }

    static Collection<Map<String, Object>> newListOfMaps(Field f, Map<String, Object> ctx) {
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

    <T, R> Simples.UExtracting<T, Map<String, Object>, R> of(String extName, Function<T, R> getter) {
        Field f = Field.Factory.of(extName);
        return Simples.Builder.uniExtractingOf(f, getter).withWriterJF(UWRITER);
    }
}
