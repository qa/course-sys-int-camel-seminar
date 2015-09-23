package com.redhat.brq.integration.camel;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.BindyType;
import org.apache.camel.model.dataformat.JaxbDataFormat;
import org.apache.camel.model.rest.RestBindingMode;

import com.redhat.brq.integration.camel.exception.ShipmentFailedException;
import com.redhat.brq.integration.camel.model.Order;
import com.redhat.brq.integration.camel.model.OrderItem;
import com.redhat.brq.integration.camel.service.OrderRepository;
import com.redhat.brq.integration.camel.service.OrderStatusProvider;

public class OrderProcessRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        restConfiguration()
            .component("restlet") // use camel-restlet component as rest api provider
            .bindingMode(RestBindingMode.json) // binding json to/from POJO (DTO)
            .port(8080)

            .dataFormatProperty("prettyPrint", "true")
            .dataFormatProperty("include", "NON_NULL") //
            .dataFormatProperty("json.in.disableFeatures", "FAIL_ON_UNKNOWN_PROPERTIES");

        rest("/orders").consumes("application/json").produces("application/json")
            .post().type(Order.class).to("direct:new-order")
            .get("/{orderId}").outType(Order.class).to("direct:find-order");


        // TASK-1
        // Finish new-order route.
        //
        // This route saves new Order to repository, sets response headers and returns.
        // In following TASK-3 the route request order issuing process.
        //
        // 1 - save Order received in in.message body (use OrderRepository class)
        // 2 - set HTTP response code to 201, see Exchange.HTTP_RESPONSE_CODE
        // 3 - set Location response header with content '/orders/$order.id'
        // 4 - set response body to null (no need for body for 201 response)
        // 5 - you can test your route by HTTP POST request with valid body
        // 6 - route message to issue-order route
        from("direct:new-order").id("new-order")
            .bean(OrderRepository.class, "create")
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(201))
            .setHeader("Location", simple("/orders/${body.id}"))
            .to("seda:issue-order") // return immediately
            .setBody(constant(null)); // return empty body for 201 Created response;


        // TASK-2
        // Finish find-order route.
        //
        // This route tries to find Order in repository.
        //
        // 1 - finish OrderRepository class (see hint)
        // 2 - you need to set orderId property of value of orderId header
        // 3 - retrieve Order by orderId (user OrderRepository class)
        // 4 - if retrieved Order is null, return HTTP response 404
        // 5 - you can test your route by HTTP GET request
        from("direct:find-order").id("find-order")
            .setProperty("orderId", simple("${header.orderId}"))
            .bean(OrderRepository.class, "get")
            .choice()
            .when(body().isNull()).setHeader(Exchange.HTTP_RESPONSE_CODE, constant(404))
            .end();


        // TASK-3
        // Finish issue-order route.
        //
        // This route set status of order to 'in process' and sends request to the Inventory system.
        //
        // 1 - change direct endpoint to asynchronous SEDA endpoint, be aware of callers!
        // 2 - set status of Order to inProcess (use OrderStatusProvider class and finish it)
        //   - you need to set orderId property like in TASK-2, but where you can find orderId now?
        // 3 - you can log order: .log("Issuing new order: ${body}")
        // 4 - you need to save order item to file for Inventory system, so do body transformation
        // 5 - marshal the body (list of items) to CSV
        //   - use camel-bindy, add the dependency to pom.xml (see a hint there)
        //   - you need to annotate OrderItem class with bindy annotations (see hints there)
        // 6 - route the CSV to file endpoint
        //   - do not use hard coded path, use {{endpoint.file.baseUrl}} do define path by system variable
        //     - see setting of ${endpoint.file.baseUrl} in maven properties and camel maven plugin
        //   - you have to set the name of the file to orderId, see properties of file endpoint of camel
        // 7 - you can test the route by IssueOrderRouteTest, the test should pass if your route is correct
        from("seda:issue-order").id("issue-order")
            .setProperty("orderId", simple("${body.id}"))
            .bean(OrderStatusProvider.class, "inProcess")
            .log("Issuing new order: ${body}")
            .transform().simple("${body.items}")
            .marshal().bindy(BindyType.Csv, OrderItem.class)
            .to("file://{{endpoint.file.baseUrl}}/inbox/inventory?fileName=${property.orderId}");


        // TASK-4
        // Finish receive-inventory route.
        //
        // This route receives response from the Inventory system and handles when item cannot be reserved.
        //
        // Finish receive-inventory route.
        // 1 - set correct path to read the response from Inventory system
        // 2 - you need to set orderId property like in TASK-2, but where you can find orderId now?
        // 3 - split the received file by '\n', use streaming mode
        // 4 - test each line, if it contains ';0;', throw ItemNotReservedException
        //   - you need to set the splitter to stop on exception
        // 5 - route to accounting route as last step of the route
        // 6 - you can test the route by ReceiveInventoryRouteTest

        // TASK-5
        // Handle the ItemNotReservedException thrown in TASK-4
        // 1 - handle(!) the exception by .onException clause
        // 2 - you can log the error: .log("Items for order::${header.CamelFileName} cannot be reserved")
        // 3 - set order status to 'reservation not possible' (use OrderStatusProvider class)
        // 4 - finish test for the error state in ReceiveInventoryRouteTest class
        //   - see the hints in the test class
        // 5 - check your updates and your test
        from("file://INVALID_PATH").id("receive-inventory")
            .log("TASK-4::receive-inventory route logic: ${body}");


        // TASK-6
        // Finish accounting route.
        //
        // This route sends an order to Accounting systems and handles response.
        //
        // 1 - retrieve Order by id (use OrderRepository)
        //   - you should have orderId property already propagated from caller route
        // 2 - marshal Order to JSON, use JsonLibrary.Jackson
        // 3 - send POST request to Accounting system
        //   - you have to remove all exchange headers not to be send in HTTP request
        //   - use https4 component, how to tell it should use POST verb?
        //     - you need to set BASIC auth, see authMethod, authenticationPreemptive, authUsername, authPassword params
        //     - you need to use SSL context defined in camel-context.xml, see sslContextParameters parameter
        // 4 - you will need to convert received body to String class
        // 5 - you can log received JSON: .log("Received Accounting response: ${body}")
        // 6 - use processor to verify if returned response has error status INVALID
        //   - call processor by .process(new AccountingResponseProcessor())
        //   - convert dummy AccountingResponseProcessor class to camel processor, see hints
        //   - if message contains '"status":"INVALID"', throw InvalidAccountingException in the processor
        // 7 - route to shipment route
        // 8 - you can test your route by AccountingRouteTest

        // TASK-7
        // Handle the InvalidAccountingException thrown in TASK-6
        // 1 - handle(!) the exception by .onException clause
        // 2 - you can log the error: .log("Invalid accounting for order ${property.orderId}")
        // 3 - set order status to 'accounting invalid' (use OrderStatusProvider class)
        // 4 - finish test for the error state in AccountingRouteTest class
        //   - see hints in the test class
        // 5 - check your updates and your test
        from("direct:accounting").id("accounting")
            .log("TASK-6::accounting route logic: ${body}");


        // TASK-8
        // Finish shipment route
        //
        // This route sends an address of a order to the Shipment system.
        //
        // 1 - retrieve Order by id (use OrderRepository)
        //   - you should have orderId property already propagated from caller route
        // 2 - convert message body from Order to Address (order.address)
        // 3 - marshal body to XML
        //   - you need to create JaxbDataFormat and set 'com.redhat.brq.integration.camel.model' as context path
        //   - you need to create jaxb.index file with list of names of classes which can be mashalled by JAXB
        //     - create it in /src/main/resources/com/redhat/brq/integration/camel/model/
        //   - use created JaxbDataFormat for marshalling
        //   - see Address class, JAXB annotations are already present
        // 4 - send XML body to SHPMNT.REQ activemq queue
        //   - you need to propagate orderId to the endpoint
        //   - HINT: only headers are propagated, exchange properties not, so...
        // 5 - you can test your route by ShipmentRouteTest
        JaxbDataFormat jaxb;
        from("direct:shipment").id("shipment")
            .log("TASK-8::shipment route logic: ${body}");


        // TASK-9
        // Finish ReceiveShipmentRouteTest for this route, see hints in the test class.
        //
        // This route receives response from the Shipment system.
        from("activemq:queue:SHPMNT.RESP").id("receive-shipment")
            .onException(ShipmentFailedException.class).handled(true)
                .log("Order ${header.orderId} cannot be shipped")
                .bean(OrderStatusProvider.class, "cannotBeShipped")
            .end()

            .log("Received Shipment Response: ${body}")
            .setProperty("orderId", simple("${header.orderId}"))
            .choice()
                .when(body().contains("FAIL")).throwException(new ShipmentFailedException())
            .end()
            .to("direct:confirm");


        from("direct:confirm").id("confirm")
            .bean(OrderStatusProvider.class, "confirm");

        // TASK-10
        // Finish integration test with PaxExam, see hints in IntegrationTest class.
    }
}
