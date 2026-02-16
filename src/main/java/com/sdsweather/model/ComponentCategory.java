package com.sdsweather.model;

public class ComponentCategory {

    public String categoryId;
    public String categoryName;
    public String createdAt;

    @Override
    public String toString() {
        return categoryName;
    }
}
