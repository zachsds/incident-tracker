package com.sdsweather.model;

public class Unit {

    public String unitId;
    public String unitType;
    public String stockNumber;
    public String title;
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
