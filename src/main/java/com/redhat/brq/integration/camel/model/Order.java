package com.redhat.brq.integration.camel.model;

import java.util.List;

public class Order {

    private long id;
    private List<OrderItem> items;
    private Address address;
    private OrderStatus status;

    public Order() {
    }

    public Order(long id, List<OrderItem> items, Address address, OrderStatus status) {
        this.id = id;
        this.items = items;
        this.address = address;
        this.status = status;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Order [id=" + id + ", items=" + items + ", address=" + address  + "]";
    }
}
