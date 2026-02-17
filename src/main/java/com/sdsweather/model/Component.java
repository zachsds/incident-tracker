package com.sdsweather.model;

/**
 * Component - Data model representing a hardware component tracked in the system.
 *
 * Components belong to categories and are linked to incidents to identify which
 * parts failed. The activeFlag allows retiring components without losing historical data.
 *
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-02-16
 */
public class Component {

    /** Unique identifier for this component */
    public String componentId;

    /** ID of the category this component belongs to */
    public String categoryId;

    /** Display name of the component (e.g., "ESP8266", "Power Supply") */
    public String componentName;

    /** Manufacturer of the component */
    public String manufacturer;

    /** Model or part number */
    public String modelNumber;

    /** 1 = active (selectable in incidents), 0 = inactive (retired) */
    public int activeFlag;

    /** ISO timestamp of when this component was registered */
    public String createdAt;

    @Override
    public String toString() {
        return componentName;
    }
}
