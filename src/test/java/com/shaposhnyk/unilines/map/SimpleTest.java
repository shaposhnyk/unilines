package com.shaposhnyk.unilines.map;

import com.shaposhnyk.unilines.UBiPipeline;
import com.shaposhnyk.unilines.UField;
import com.shaposhnyk.unilines.UTriConsumer;
import com.shaposhnyk.unilines.builders.ExtractingBuilder;
import com.shaposhnyk.unilines.builders.UCField;
import com.shaposhnyk.unilines.map.helpers.MyObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.function.BiConsumer;

import static org.hamcrest.CoreMatchers.equalTo;

/**
 * UField convertion tests
 */
public class SimpleTest extends ConverterBase {

    @Test
    public void simpleConverter() {
        UBiPipeline<MyObject, Map<String, Object>> conv = simpleInt();
        assertConvertionOnSome(conv, equalTo("Some"));
    }

    @Test
    public void simpleConverterWithCondition() {
        UCField.Simple<MyObject, Map<String, Object>> sconv = simpleInt();
        UBiPipeline<MyObject, Map<String, Object>> conv = sconv.filterJS(s -> s.getName() != null);

        assertConvertionOnSome(conv, equalTo("Some"));
        assertNoConvertionOnNull(conv);
    }

    @Test
    public void extractingWithDecorator() {
        BiConsumer<String, Map<String, Object>> writer = (s, ctx) -> ctx.put(fInt().externalName(), s);

        ExtractingBuilder<MyObject, Map<String, Object>, String> conv = UCField.Builder
                .extractingOf(fInt(), MyObject::getName)
                .withWriterJ(writer)
                .decorateJ(String::toUpperCase);

        assertConvertionOnSome(conv, equalTo("SOME"));
        assertNoConvertionOnNull(conv);
    }

    @Test
    public void univExtractorWithDecorator() {
        UTriConsumer<UField, Object, Map<String, Object>> writer = (f, s, ctx) -> ctx.put(f.externalName(), s);

        ExtractingBuilder<MyObject, Map<String, Object>, String> conv = UCField.Builder
                .uniExtractingOf(fInt(), MyObject::getName)
                .withWriterJF(writer)
                .decorateJ(String::toUpperCase);

        assertConvertionOnSome(conv, equalTo("SOME"));
        assertNoConvertionOnNull(conv);
    }

    @Test
    public void univExtractorWithWriter() {
        UTriConsumer<UField, Object, Map<String, Object>> writer = (f, s, ctx) -> ctx.put(f.externalName(), s);


        ExtractingBuilder<MyObject, Map<String, Object>, String> conv = UCField.Builder
                .uniExtractingOf(fInt(), MyObject::getName)
                .withWriterJF(writer)
                .decorateJ(String::toUpperCase);

        assertConvertionOnSome(conv, equalTo("SOME"));
        assertNoConvertionOnNull(conv);
    }

    @Test
    public void fUnivExtractorWithDecorator() {
        UTriConsumer<UField, Object, Map<String, Object>> writer = (f, s, ctx) -> ctx.put(f.externalName(), s);

        UBiPipeline<MyObject, Map<String, Object>> conv = UCField.Builder
                .fUniExtractingOf(fInt(), (UField f, MyObject o) -> o.getName())
                .withWriterJF(writer)
                .decorateJ(String::toUpperCase);

        assertConvertionOnSome(conv, equalTo("SOME"));
        assertNoConvertionOnNull(conv);
    }

    @Test
    public void fUnivExtractorWithTransformer() {
        UTriConsumer<UField, Object, Map<String, Object>> writer = (f, s, ctx) -> ctx.put(f.externalName(), s);

        UBiPipeline<MyObject, Map<String, Object>> conv = UCField.Builder
                .fUniExtractingOf(fInt(), (UField f, MyObject o) -> o.getArray())
                .decorateJ(String::toUpperCase)
                .mapJ(s -> Arrays.asList(s.split(",")))
                .withWriterJF(writer);

        assertConvertionOnSome(conv, equalTo(Arrays.asList("SOME1", "SOME2")));
        assertNoConvertionOnNull(conv);

        // place of writer is unimportant
        UBiPipeline<MyObject, Map<String, Object>> conv1 = UCField.Builder
                .fUniExtractingOf(fInt(), (UField f, MyObject o) -> o.getArray())
                .withWriterJF(writer)
                .decorateJ(String::toLowerCase)
                .mapJ(s -> Arrays.asList(s.split(",")));

        assertConvertionOnSome(conv1, equalTo(Arrays.asList("some1", "some2")));
        assertNoConvertionOnNull(conv1);
    }

    @Test
    public void fUnivExtractorWithIgnoringErrors() {
        UTriConsumer<UField, Object, Map<String, Object>> writer = (f, s, ctx) -> ctx.put(f.externalName(), s);

        UCField.UExtracting<MyObject, Map<String, Object>, ?> conv = UCField.Builder
                .fUniExtractingOf(fInt(), (UField f, MyObject o) -> o.getNumberLike())
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
