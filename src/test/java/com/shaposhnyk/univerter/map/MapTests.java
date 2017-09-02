package com.shaposhnyk.univerter.map;

import com.shaposhnyk.univerter.Builders;
import com.shaposhnyk.univerter.Field;
import com.shaposhnyk.univerter.ObjectBuilders;
import com.shaposhnyk.univerter.TriConsumer;
import org.junit.Test;

import java.util.Map;
import java.util.function.Function;

/**
 * Created by vlad on 30.08.17.
 */
public class MapTests extends ConvertorBase {

    TriConsumer<Field, Object, Map<String, Object>> UWRITER = (f, s, ctx) -> ctx.put(f.externalName(), s);

    @Test
    public void univExtractorWithDecorator() {
        MyObject input = null;
        MySubObject subObject = null;
        Map<String, Object> ctxType = null;

        Field root = Field.Factory.of("root");
        Field mySubObject = Field.Factory.of("mySubObject");
        ObjectBuilders.Factory.of(root, input, ctxType)
                .field(of("name", MyObject::getName).withJDecorator(String::toUpperCase))
//                //.field(of("numberField", MyObject::getNumberLike)
//                //        .withJTransformer((String s) -> Integer.valueOf(s))
//                //        .ignoreErrors()
//                //)
//                //.field(of("arrayField", MyObject::getArray).withJTransformer((String s) -> Arrays.asList(s.split("\\,"))))
//                .field(
//                        ObjectBuilders.Factory.of(mySubObject, subObject, ctxType)
//                                //.withInputTransform(MyObject::getSubObejct)
//                                //.field(of("id", MySubObject::getValue).withJTransformer((Integer i) -> i.toString()))
//                                .field(of("name", MySubObject::getName).withJDecorator(String::toLowerCase))
//                                .build()
//                )
//                .build();
        ;

        //assertConvertionOnSome(conv, equalTo("SOME"));
        //assertNoConvertionOnNull(conv);
    }

    private <R> Builders.UExtracting<MyObject, Map<String, Object>, R> of(String extName, Function<MyObject, R> getter) {
        Field f = Field.Factory.of(extName);
        return Builders.Factory.uniExtractingOf(f, getter).withJFWriter(UWRITER);
    }


}
