package com.redhat.brq.integration.camel.model;

public class OrderStatus {

    private String resolution;
    private String description;

    public OrderStatus(String resolution, String description) {
        this.resolution = resolution;
        this.description = description;
    }

    public String getResolution() {
        return resolution;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "OrderStatus [resolution=" + resolution + ", description=" + description + "]";
    }
}
