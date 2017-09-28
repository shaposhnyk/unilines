package com.shaposhnyk.unilines.map.helpers;

/**
 * Test object
 */
public final class MyObject {
    private final String name;
    private final int value;
    private final MySubObject subObject;

    public MyObject(String name, int value) {
        this.name = name;
        this.value = value;
        this.subObject = new MySubObject("s" + name, value / 2);
    }

    public String getName() {
        return name;
    }

    public String getArray() {
        return name == null ? null : (name + "1," + name + "2");
    }

    public String getNumberLike() {
        return name == null ? "notANumber" : "3";
    }

    public int getValue() {
        return value;
    }

    public MySubObject getSubObject() {
        return subObject;
    }

}
