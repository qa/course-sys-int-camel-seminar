package com.redhat.brq.integration.camel.test;

import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.RouteDefinition;
import org.junit.Before;
import org.junit.Test;

import com.redhat.brq.integration.camel.model.Order;
import com.redhat.brq.integration.camel.service.OrderRepository;

public class AccountingRouteTest extends OrderProcessRouteTest {

    @EndpointInject(uri = "direct:accounting")
    private ProducerTemplate producer;

    @EndpointInject(uri = "mock:shipment")
    private MockEndpoint shipment;

    public String accountingStatus;

    @Before
    public void interceptHttp4() throws Exception {
        RouteDefinition route = context.getRouteDefinitions().get(0);
        route.adviceWith(context, new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                interceptSendToEndpoint("https4://*")
                    .skipSendToOriginalEndpoint()
                    .process(exchange -> {
                        String mockResponse = "{\"status\":\"" + accountingStatus + "\"}";
                        exchange.getOut().setBody(mockResponse);
                    });

                interceptSendToEndpoint("direct:shipment")
                    .skipSendToOriginalEndpoint()
                    .to("mock:shipment");
            }
        });
    }

    @Test
    public void testReservedItems() throws Exception {
        accountingStatus = "ISSUED";

        shipment.expectedMessageCount(1);
        producer.sendBodyAndProperty("-", "orderId", "1");

        shipment.assertIsSatisfied();
        shipment.reset();
    }

    // TASK-7
    // 1 - convert to test method
    // 2 - shipment should receive no message - set asserts
    // 3 - send body and property (why / which exchange property?)
    // 4 - check order status
    public void testReservationFailed() throws Exception {
        accountingStatus = "INVALID";

        // TASK-7: set expected message count to shipment mock endpoint
        // TASK-7: send body and property (why / which exchange property?) with producer

        // TASK-7: verify if mock asserts are satisfied
        shipment.reset(); // reset mock endpoint, is used in more then one test method

        Order order = OrderRepository.get(1L);
        // TASK-5: check that order.status.resolution is 'CANCELLED'
        // TASK-5: check that order.status.description is 'Invalid accounting'
    }

    @Override
    protected String getTestedRouteName() {
        return "accounting";
    }
}
