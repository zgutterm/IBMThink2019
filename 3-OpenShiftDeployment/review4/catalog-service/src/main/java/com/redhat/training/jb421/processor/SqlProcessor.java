package com.redhat.training.jb421.processor;


import com.redhat.training.jb421.model.CatalogItem;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class SqlProcessor implements Processor {
    @Override
    public void process(Exchange exchange) throws Exception {
        CatalogItem catalogItem = exchange.getIn().getBody(CatalogItem.class);
        exchange.getIn().setHeader("catalog_id",catalogItem.getId());
        exchange.getIn().setHeader("catalog_author",catalogItem.getAuthor());
        exchange.getIn().setHeader("catalog_description",catalogItem.getDescription());
        exchange.getIn().setHeader("catalog_vendor_id",catalogItem.getVendorId());

    }
}
