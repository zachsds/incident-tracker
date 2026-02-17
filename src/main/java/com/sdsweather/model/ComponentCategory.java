package com.sdsweather.model;

/**
 * ComponentCategory - Data model representing a grouping of hardware components.
 *
 * Categories organize components into logical groups (e.g., "Sensors", "Power",
 * "Communication") to make incident reporting easier. Users select a category
 * first, then choose the specific component from that group.
 *
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-02-16
 */
public class ComponentCategory {

    /** Unique identifier for this category */
    public String categoryId;

    /** Display name of the category (e.g., "Sensors", "Power") */
    public String categoryName;

    /** ISO timestamp of when this category was created */
    public String createdAt;

    @Override
    public String toString() {
        return categoryName;
    }
}
