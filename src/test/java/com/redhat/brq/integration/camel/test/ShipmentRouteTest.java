package com.redhat.brq.integration.camel.test;

import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.RouteDefinition;
import org.junit.Test;

import com.redhat.brq.integration.camel.model.Order;
import com.redhat.brq.integration.camel.service.OrderRepository;

public class ShipmentRouteTest extends OrderProcessRouteTest {

    @EndpointInject(uri = "direct:shipment")
    private ProducerTemplate producer;

    @EndpointInject(uri = "mock:shipment")
    private MockEndpoint shipment;

    @Test
    public void testShipment() throws Exception {
        RouteDefinition route = context.getRouteDefinitions().get(0);
        route.adviceWith(context, new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                interceptSendToEndpoint("activemq://*")
                    .skipSendToOriginalEndpoint()
                    .to("mock:shipment");
            }
        });

        shipment.expectedMessageCount(1);
        producer.sendBodyAndProperty("-", "orderId", "1");

        shipment.assertIsSatisfied();
        String body = shipment.getExchanges().get(0).getIn().getBody(String.class);

        JAXBContext context = JAXBContext.newInstance(Order.class);
        Marshaller m = context.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        StringWriter writer = new StringWriter();
        m.marshal(OrderRepository.get(1L).getAddress(), writer);

        assertEquals(writer.toString(), body);
    }

    @Override
    protected String getTestedRouteName() {
        return "shipment";
    }
}
