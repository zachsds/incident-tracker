package com.sdsweather.model;

/**
 * Unit - Data model representing a weather monitoring unit in the system.
 *
 * Units are the primary entities tracked for incidents. Two types exist:
 *   STOCK    - Units in inventory, not yet deployed. Identified by stock number.
 *   DEPLOYED - Units installed at a location. Identified by a descriptive title.
 *
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-02-16
 */
public class Unit {

    /** Unique identifier (UUID) for this unit */
    public String unitId;

    /** Type of unit: "STOCK" or "DEPLOYED" */
    public String unitType;

    /** Stock number for STOCK units (e.g., "0001"). Null for DEPLOYED. */
    public String stockNumber;

    /** Descriptive title for DEPLOYED units (e.g., "Airport Station"). Null for STOCK. */
    public String title;

    /** ISO timestamp of when this unit was registered */
    public String createdAt;

    @Override
    public String toString() {
        if ("STOCK".equals(unitType)) {
            return "STOCK - " + stockNumber + " (" + unitId + ")";
        } else {
            return title + " (" + unitId + ")";
        }
    }
}
