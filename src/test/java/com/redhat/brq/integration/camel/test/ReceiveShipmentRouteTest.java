package com.redhat.brq.integration.camel.test;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.RouteDefinition;

import com.redhat.brq.integration.camel.model.Order;
import com.redhat.brq.integration.camel.service.OrderRepository;

// TASK-9
// Finish test class
public class ReceiveShipmentRouteTest extends OrderProcessRouteTest {

    // TASK-9: inject endpoint activemq:queue:SHPMNT.RESP
    private ProducerTemplate producer;

    // TASK-9: inject endpoint mock:confirm
    private MockEndpoint confirm;

    // TASK-9: convert to test method
    public void testReservedItems() throws Exception {
        RouteDefinition route = context.getRouteDefinitions().get(0);
        // TASK-9: advice route with
        // 1 - intercept send to endpoint direct:confirm
        // 2 - skip send to this direct endpoint
        // 3 - instead send to mock:confirm
        // route.advice...


        confirm.expectedMessageCount(1);
        producer.sendBodyAndHeader("<delivery status=\"OK\"/>", "orderId", "1");

        confirm.assertIsSatisfied();
    }

    // TASK-9: convert to test method
    public void testReservationFailed() throws Exception {
        // TASK-9: send body with FAIL delivery status and orderId=1 header

        Thread.sleep(3000); // wait for processing

        Order order = OrderRepository.get(1L);
        // TASK-9: check that order.status.resolution is 'CANCELLED'
        // TASK-9: check that order.status.description is 'The order cannot be shipped'
    }

    @Override
    protected String getTestedRouteName() {
        return "receive-shipment";
    }
}
