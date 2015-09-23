package com.redhat.brq.integration.camel.test;

import java.io.File;

import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.brq.integration.camel.service.OrderRepository;

public class IssueOrderRouteTest extends OrderProcessRouteTest {

    @EndpointInject(uri = "seda:issue-order")
    private ProducerTemplate producer;

    @Before
    @After
    public void cleanInbox() {
        deleteDirectory("inbox");
    }

    @Test
    public void testIssueOrder() throws Exception {
        producer.sendBody(OrderRepository.get(1L));

        // wait a while to let the file be created
        Thread.sleep(2000);

        File target = new File("inbox/inventory/1");
        assertTrue("Inventory file should have been created", target.exists());

        String content = context.getTypeConverter().convertTo(String.class, target);
        assertEquals("1;2\n2;2\n", content);
    }

    @Override
    protected String getTestedRouteName() {
        return "issue-order";
    }
}
