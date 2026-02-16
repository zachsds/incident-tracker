package com.sdsweather.model;

public class Component {

    public String componentId;
    public String categoryId;
    public String componentName;
    public String manufacturer;
    public String modelNumber;
    public int activeFlag;
    public String createdAt;

    @Override
    public String toString() {
        return componentName;
    }
}
