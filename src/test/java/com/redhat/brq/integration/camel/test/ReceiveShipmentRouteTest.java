package com.redhat.brq.integration.camel.test;

import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.RouteDefinition;
import org.junit.Test;

import com.redhat.brq.integration.camel.model.Order;
import com.redhat.brq.integration.camel.service.OrderRepository;

// TASK-9
// Finish test class
public class ReceiveShipmentRouteTest extends OrderProcessRouteTest {

    // TASK-9: inject endpoint activemq:queue:SHPMNT.RESP
    @EndpointInject(uri = "activemq:queue:SHPMNT.RESP")
    private ProducerTemplate producer;

    // TASK-9: inject endpoint mock:confirm
    @EndpointInject(uri = "mock:confirm")
    private MockEndpoint confirm;

    // TASK-9: convert to test method
    @Test
    public void testReservedItems() throws Exception {
        RouteDefinition route = context.getRouteDefinitions().get(0);
        // TASK-9: advice route with
        // 1 - intercept send to endpoint direct:confirm
        // 2 - skip send to this direct endpoint
        // 3 - instead send to mock:confirm
        // route.advice...
        route.adviceWith(context, new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                // intercept sending to direct:accounting and detour to our processor instead
                interceptSendToEndpoint("direct:confirm")
                    // skip sending to the real http when the detour ends
                    .skipSendToOriginalEndpoint()
                    .to("mock:confirm");
            }
        });

        confirm.expectedMessageCount(1);
        producer.sendBodyAndHeader("<delivery status=\"OK\"/>", "orderId", "1");

        confirm.assertIsSatisfied();
    }

    // TASK-9: convert to test method
    @Test
    public void testReservationFailed() throws Exception {
        // TASK-9: send body with FAIL delivery status and orderId=1 header
        producer.sendBodyAndHeader("<delivery status=\"FAIL\"/>", "orderId", "1");

        Thread.sleep(3000); // wait for processing

        Order order = OrderRepository.get(1L);
        // TASK-9: check that order.status.resolution is 'CANCELLED'
        // TASK-9: check that order.status.description is 'The order cannot be shipped'
        assertEquals("CANCELLED", order.getStatus().getResolution());
        assertEquals("The order cannot be shipped", order.getStatus().getDescription());
    }

    @Override
    protected String getTestedRouteName() {
        return "receive-shipment";
    }
}
