package com.redhat.brq.integration.camel;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import com.redhat.brq.integration.camel.exception.InvalidAccountingException;

// TASK-6
// 1- implement camel Processor process(Exchange) method
// 2 - test whether in.body contains '"status":"INVALID"'
//   - if yes, throw InvalidAccountingException
public class AccountingResponseProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        String body = exchange.getIn().getBody(String.class);
        if (body.contains("\"status\":\"INVALID\"")) {
            throw new InvalidAccountingException();
        }
    }
}
