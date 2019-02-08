package com.redhat.training.jb421.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.jackson.JacksonDataFormat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.LinkedHashMap;

public class VendorProcessor implements Processor {
    @Override
    public void process(Exchange exchange) throws Exception {

        String vendorJSON = exchange.getIn().getBody(String.class);

        ObjectMapper mapper = new ObjectMapper();
        JacksonDataFormat dataFormat = new JacksonDataFormat(mapper, Object.class);
        InputStream is = new ByteArrayInputStream(vendorJSON.getBytes("UTF-8"));
        LinkedHashMap<String,Object> vendorVo = (LinkedHashMap<String,Object>) dataFormat.unmarshal(null,is);

        String vendorName = vendorVo.get("name").toString();

        exchange.getIn().setHeader("vendor_name", vendorName);
    }
}
