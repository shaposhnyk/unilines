package com.shaposhnyk.univerter.map;

/**
 * Test sub-object
 */
final class MySubObject {
    private final String name;
    private final int value;

    MySubObject(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

}
