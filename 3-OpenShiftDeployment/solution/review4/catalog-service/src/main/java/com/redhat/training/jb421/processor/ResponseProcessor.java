package com.redhat.training.jb421.processor;

import com.redhat.training.jb421.model.ResponseVO;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class ResponseProcessor implements Processor {
    @Override
    public void process(Exchange exchange) throws Exception {

        ResponseVO vo = new ResponseVO();
        vo.setId(Integer.valueOf(exchange.getIn().getHeader("catalog_id").toString()));
        vo.setAuthor(exchange.getIn().getHeader("catalog_author").toString());
        vo.setDescription(exchange.getIn().getHeader("catalog_description").toString());
        vo.setVendorName(exchange.getIn().getHeader("vendor_name").toString());

        exchange.getOut().setBody(vo);

    }
}
