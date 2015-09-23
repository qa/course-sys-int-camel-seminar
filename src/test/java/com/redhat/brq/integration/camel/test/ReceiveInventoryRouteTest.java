package com.redhat.brq.integration.camel.test;

import java.io.File;

import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.RouteDefinition;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.brq.integration.camel.model.Order;
import com.redhat.brq.integration.camel.service.OrderRepository;

public class ReceiveInventoryRouteTest extends OrderProcessRouteTest {

    @EndpointInject(uri = "file://{{endpoint.file.baseUrl}}/outbox/inventory")
    private ProducerTemplate producer;

    @EndpointInject(uri = "mock:accounting")
    private MockEndpoint accounting;

    @Before
    @After
    public void cleanInbox() {
        deleteDirectory("outbox");
    }

    @Test
    public void testReservedItems() throws Exception {
        RouteDefinition route = context.getRouteDefinitions().get(0);
        route.adviceWith(context, new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                // intercept sending to direct:accounting and detour to our processor instead
                interceptSendToEndpoint("direct:accounting")
                    // skip sending to the real http when the detour ends
                    .skipSendToOriginalEndpoint()
                    .to("mock:accounting");
            }
        });
        accounting.expectedMessageCount(1);

        producer.sendBodyAndHeader("1;2;10\n2;2;12\n", "CamelFileName", "1");

        // wait a while to let the file be consumed
        Thread.sleep(2000);

        accounting.assertIsSatisfied();
        accounting.getReceivedExchanges().get(0).getProperties().get("orderId").equals("1");

        File target = new File("outbox/inventory/1");
        assertTrue("Inventory file should have been consumed", !target.exists());
    }

    // TASK-5
    // 1 - convert to test method
    // 2 - send invalid (item not available) body with the producer
    //   - do not forget to set file name to '1'
    // 3 - assert status of order with id 1
    // 4 - check that file was consumed
    @Test
    public void testReservationFailed() throws Exception {
        // TASK-5: send body and header with the producer
        producer.sendBodyAndHeader("1;2;0\n2;0;1\n", "CamelFileName", "1");

        // wait a while to let the file be consumed
        Thread.sleep(2000);

        Order order = OrderRepository.get(1L);
        // TASK-5: check that order.status.resolution is 'CANCELLED'
        // TASK-5: check that order.status.description is 'Not enough items in an inventory'
        assertEquals("CANCELLED", order.getStatus().getResolution());
        assertEquals("Not enough items in an inventory", order.getStatus().getDescription());

        File target = new File("outbox/inventory/1");
        // TASK-5: check that file does not exists
        assertTrue("Inventory file should have been consumed", !target.exists());
    }

    @Override
    protected String getTestedRouteName() {
        return "receive-inventory";
    }
}
