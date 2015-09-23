package com.redhat.brq.integration.camel.service;

import org.apache.camel.ExchangeProperty;

import com.redhat.brq.integration.camel.model.Order;
import com.redhat.brq.integration.camel.model.OrderStatus;

// TASK-3
// 1 - map long orderId of every method to property.orderId of camel exchange
public final class OrderStatusProvider {

    private static final String RESOLUTION_CANCELLED = "CANCELLED";
    private static final String RESOLUTION_IN_PROCESS = "IN PROCESS";
    private static final String RESOLUTION_CONFIRMED = "CONFIRMED";


    public static void inProcess(@ExchangeProperty("orderId") long orderId) {
        setStatus(orderId, RESOLUTION_IN_PROCESS, "Contacting Inventory, Accounting and Shipment");
    }

    public static void reservationNotPossible(@ExchangeProperty("orderId") long orderId) {
        setStatus(orderId, RESOLUTION_CANCELLED, "Not enough items in an inventory");
    }

    public static void accountingInvalid(@ExchangeProperty("orderId") long orderId) {
        setStatus(orderId, RESOLUTION_CANCELLED, "Invalid accounting");
    }

    public static void cannotBeShipped(@ExchangeProperty("orderId") long orderId) {
        setStatus(orderId, RESOLUTION_CANCELLED, "The order cannot be shipped");
    }

    public static void confirm(@ExchangeProperty("orderId") long orderId) {
        setStatus(orderId, RESOLUTION_CONFIRMED, "The order is ready for shipment");
    }

    private static void setStatus(long orderId, String resolution, String description) {
        Order order = OrderRepository.get(orderId);
        if (order != null) {
            order.setStatus(new OrderStatus(resolution, description));
        }
    }

    private OrderStatusProvider() {
    }
}
