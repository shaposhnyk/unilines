package com.shaposhnyk.univerter

data class LDAPAttr(val name: String, val value: Any)

data class LDAPObject(val attributes: Collection<LDAPAttr>) {
    fun get(internalName: String): Any {
        return attributes.filter { a -> a.name == internalName }
                .first().
    }
}

interface JSONObject {
    fun set(name: String, value: Any)
    fun createArray(name: String): JSONObject
    fun createObject(name: String): JSONObject
}


fun main(args: Array<String>) {
    println("Hello, World")

    val f = Field.of("root");
    val id = Field.of("accId", "id")
    val so = Field.of("subobject", "subobject")

    val repeater: Convertor<LDAPObject, JSONObject> = repeater(hof(f) // notinitialized convertor C<Object,Object>
            .transformInput(::getObjectFrom) // NI HC<Exchange,*,List<LDAPObject>>
            .transformContext { _: Any -> newObjectNode() } // C<Exchange,JSONObject,List<LDAPObject>>
    )
    repeater // C<Exchange,JSONObject,LDAPObject>>
            .hierarchical(
                    ldapField(id), // C<LDAPObject, JSONObject>
                    objecter(so)
                            .hierarchical(
                                    ldapField(id)
                            )
            )
}

fun ldapField(id: Field): Convertor<LDAPObject, JSONObject> {
    return of(id, {
        source: LDAPObject, ctx: JSONObject
        ->
        ctx.set(id.externalName(), source.get(id.internalName()))
    })
}

fun objecter(so: Field): Convertor<LDAPObject, JSONObject> {
    return of(so, { s: LDAPObject, c: JSONObject -> Unit }).transformContext {
        ctx: JSONObject ->
        ctx.createObject(so.externalName())
    }
}

fun <T, C> repeater(conv: Convertor<Collection<T>, C>): Convertor<T, C> {
    return SimpleConv.repeating(conv)
}

fun hof(f: Field): Convertor<Any, Any> {
    return SimpleConv.of(f)
}

fun <T, C> of(f: Field, fx: (source: T, ctx: C) -> Unit): Convertor<T, C> {
    return SimpleConv.of(f, fx);
}

fun getFromMap(map: Map<String, Any>, f: Field): String {
    map.get(f.internalName()) as String
}

fun getObjectFrom(exchange: Any): Collection<LDAPObject> {
    return listOf(LDAPObject(listOf(LDAPAttr("some", "value"))))
}

fun newObjectNode(): JSONObject {
    return "" as JSONObject;
}
