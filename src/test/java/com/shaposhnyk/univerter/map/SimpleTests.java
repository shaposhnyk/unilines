package com.shaposhnyk.univerter.map;

import com.shaposhnyk.univerter.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.function.BiConsumer;

import static org.hamcrest.CoreMatchers.equalTo;

/**
 * Field convertion tests
 */
public class SimpleTests extends ConverterBase {

    @Test
    public void simpleConverter() {
        Converter<MyObject, Map<String, Object>> conv = simpleInt();
        assertConvertionOnSome(conv, equalTo("Some"));
    }

    @Test
    public void simpleConverterWithCondition() {
        Builders.Simple<MyObject, Map<String, Object>> sconv = simpleInt();
        Converter<MyObject, Map<String, Object>> conv = sconv.filterJS(s -> s.getName() != null);

        assertConvertionOnSome(conv, equalTo("Some"));
        assertNoConvertionOnNull(conv);
    }

    @Test
    public void extractingWithDecorator() {
        BiConsumer<String, Map<String, Object>> writer = (s, ctx) -> ctx.put(fInt().externalName(), s);

        ExtractingBuilder<MyObject, Map<String, Object>, String> conv = Builders.Factory
                .extractingOf(fInt(), MyObject::getName)
                .withWriterJ(writer)
                .decorateJ(String::toUpperCase);

        assertConvertionOnSome(conv, equalTo("SOME"));
        assertNoConvertionOnNull(conv);
    }

    @Test
    public void univExtractorWithDecorator() {
        TriConsumer<Field, Object, Map<String, Object>> writer = (f, s, ctx) -> ctx.put(f.externalName(), s);

        ExtractingBuilder<MyObject, Map<String, Object>, String> conv = Builders.Factory
                .uniExtractingOf(fInt(), MyObject::getName)
                .withWriterJF(writer)
                .decorateJ(String::toUpperCase);

        assertConvertionOnSome(conv, equalTo("SOME"));
        assertNoConvertionOnNull(conv);
    }

    @Test
    public void univExtractorWithWriter() {
        TriConsumer<Field, Object, Map<String, Object>> writer = (f, s, ctx) -> ctx.put(f.externalName(), s);


        ExtractingBuilder<MyObject, Map<String, Object>, String> conv = Builders.Factory
                .uniExtractingOf(fInt(), MyObject::getName)
                .withWriterJF(writer)
                .decorateJ(String::toUpperCase);

        assertConvertionOnSome(conv, equalTo("SOME"));
        assertNoConvertionOnNull(conv);
    }

    @Test
    public void fUnivExtractorWithDecorator() {
        TriConsumer<Field, Object, Map<String, Object>> writer = (f, s, ctx) -> ctx.put(f.externalName(), s);

        Converter<MyObject, Map<String, Object>> conv = Builders.Factory
                .fUniExtractingOf(fInt(), (Field f, MyObject o) -> o.getName())
                .withWriterJF(writer)
                .decorateJ(String::toUpperCase);

        assertConvertionOnSome(conv, equalTo("SOME"));
        assertNoConvertionOnNull(conv);
    }

    @Test
    public void fUnivExtractorWithTransformer() {
        TriConsumer<Field, Object, Map<String, Object>> writer = (f, s, ctx) -> ctx.put(f.externalName(), s);

        Converter<MyObject, Map<String, Object>> conv = Builders.Factory
                .fUniExtractingOf(fInt(), (Field f, MyObject o) -> o.getArray())
                .decorateJ(String::toUpperCase)
                .mapJ(s -> Arrays.asList(s.split(",")))
                .withWriterJF(writer);

        assertConvertionOnSome(conv, equalTo(Arrays.asList("SOME1", "SOME2")));
        assertNoConvertionOnNull(conv);

        // place of writer is unimportant
        Converter<MyObject, Map<String, Object>> conv1 = Builders.Factory
                .fUniExtractingOf(fInt(), (Field f, MyObject o) -> o.getArray())
                .withWriterJF(writer)
                .decorateJ(String::toLowerCase)
                .mapJ(s -> Arrays.asList(s.split(",")));

        assertConvertionOnSome(conv1, equalTo(Arrays.asList("some1", "some2")));
        assertNoConvertionOnNull(conv1);
    }

    @Test
    public void fUnivExtractorWithIgnoringErrors() {
        TriConsumer<Field, Object, Map<String, Object>> writer = (f, s, ctx) -> ctx.put(f.externalName(), s);

        Builders.UExtracting<MyObject, Map<String, Object>, ?> conv = Builders.Factory
                .fUniExtractingOf(fInt(), (Field f, MyObject o) -> o.getNumberLike())
                .mapJ(s -> Integer.valueOf(s))
                .withWriterJF(writer)
                .decorateJ(i -> i * 2);

        assertConvertionOnSome(conv, equalTo(6));

        try {
            // this will raise an exception
            assertNoConvertionOnNull(conv);
            Assert.assertFalse("should fail with NumberFormatException", true);
        } catch (NumberFormatException e) {

        }

        // this will pass
        assertNoConvertionOnNull(conv.silenceErrors());
    }
}
