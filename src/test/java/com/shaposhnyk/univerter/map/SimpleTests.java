package com.shaposhnyk.univerter.map;

import com.shaposhnyk.univerter.Builders;
import com.shaposhnyk.univerter.Convertor;
import com.shaposhnyk.univerter.Field;
import com.shaposhnyk.univerter.TriConsumer;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.function.BiConsumer;

import static org.hamcrest.CoreMatchers.equalTo;

/**
 * Created by vlad on 30.08.17.
 */
public class SimpleTests extends ConvertorBase {

    @Test
    public void simpleConverter() {
        Convertor<MyObject, Map<String, Object>> conv = simpleInt();
        assertConvertionOnSome(conv, equalTo("some"));
    }

    @Test
    public void simpleConverterWithCondition() {
        Builders.Simple<MyObject, Map<String, Object>> sconv = simpleInt();
        Convertor<MyObject, Map<String, Object>> conv = sconv.withCondition(s -> s.getName() != null);

        assertConvertionOnSome(conv, equalTo("some"));
        assertNoConvertionOnNull(conv);
    }

    @Test
    public void extractingWithDecorator() {
        BiConsumer<String, Map<String, Object>> writer = (s, ctx) -> ctx.put(fInt().externalName(), s);

        Builders.Extracting<MyObject, Map<String, Object>, String> conv = Builders.Factory
                .extractingOf(fInt(), MyObject::getName, writer)
                .withJDecorator(s -> s.toUpperCase());

        assertConvertionOnSome(conv, equalTo("SOME"));
        assertNoConvertionOnNull(conv);
    }

    @Test
    public void univExtractorWithDecorator() {
        BiConsumer<Object, Map<String, Object>> writer = (s, ctx) -> ctx.put(fInt().externalName(), s);

        Builders.UExtracting<MyObject, Map<String, Object>, String> conv = Builders.Factory
                .uniExtractingOf(fInt(), MyObject::getName)
                .withJWriter(writer)
                .withJDecorator(s -> s.toUpperCase());

        assertConvertionOnSome(conv, equalTo("SOME"));
        assertNoConvertionOnNull(conv);
    }

    @Test
    public void univExtractorWithWriter() {
        BiConsumer<Object, Map<String, Object>> writer = (s, ctx) -> ctx.put(fInt().externalName(), s);


        Builders.UExtracting<MyObject, Map<String, Object>, String> conv = Builders.Factory
                .uniExtractingOf(fInt(), MyObject::getName)
                .withJWriter(writer)
                .withJDecorator(String::toUpperCase);

        assertConvertionOnSome(conv, equalTo("SOME"));
        assertNoConvertionOnNull(conv);
    }

    @Test
    public void fUnivExtractorWithDecorator() {
        TriConsumer<Field, Object, Map<String, Object>> writer = (f, s, ctx) -> ctx.put(f.externalName(), s);

        Convertor<MyObject, Map<String, Object>> conv = Builders.Factory
                .fUniExtractingOf(fInt(), (Field f, MyObject o) -> o.getName())
                .withJFWriter(writer)
                .withJDecorator(String::toUpperCase);

        assertConvertionOnSome(conv, equalTo("SOME"));
        assertNoConvertionOnNull(conv);
    }

    @Test
    public void fUnivExtractorWithTransformer() {
        TriConsumer<Field, Object, Map<String, Object>> writer = (f, s, ctx) -> ctx.put(f.externalName(), s);

        Convertor<MyObject, Map<String, Object>> conv = Builders.Factory
                .fUniExtractingOf(fInt(), (Field f, MyObject o) -> o.getArray())
                .withJDecorator(String::toUpperCase)
                .withJTransformer(s -> Arrays.asList(s.split("\\,")))
                .withJFWriter(writer);

        assertConvertionOnSome(conv, equalTo(Arrays.asList("SOME1", "SOME2")));
        assertNoConvertionOnNull(conv);

        // place of writer is unimportant
        Convertor<MyObject, Map<String, Object>> conv1 = Builders.Factory
                .fUniExtractingOf(fInt(), (Field f, MyObject o) -> o.getArray())
                .withJFWriter(writer)
                .withJDecorator(String::toUpperCase)
                .withJTransformer(s -> Arrays.asList(s.split("\\,")));

        assertConvertionOnSome(conv1, equalTo(Arrays.asList("SOME1", "SOME2")));
        assertNoConvertionOnNull(conv1);
    }
}
