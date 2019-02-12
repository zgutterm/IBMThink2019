package com.redhat.training.jb421.converter;

import com.redhat.training.jb421.model.CatalogItem;
import org.apache.camel.Converter;
import org.apache.camel.Exchange;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Converter
public class CatalogItemConverter {

    @Converter
    public static String catalogItemToString(CatalogItem catalogItem, Exchange exchange) {
        return catalogItem.toString();
    }
    @Converter
    public static InputStream catalogItemToIStream(CatalogItem catalogItem, Exchange exchange) {
        ByteArrayInputStream bais = new
                ByteArrayInputStream(catalogItem.toString().getBytes());
        return bais;
    }
}
