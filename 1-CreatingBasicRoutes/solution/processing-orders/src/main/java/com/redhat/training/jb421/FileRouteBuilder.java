package com.redhat.training.jb421;

import org.apache.camel.builder.RouteBuilder;

//TODO: Enable the route by extending the RouteBuilder superclass
public class FileRouteBuilder extends RouteBuilder {

    //TODO Implement the configure method
    @Override
    public void configure() throws Exception {

        from("file:orders/incoming?include=order.*xml")
                .to("file:orders/outgoing/?fileExist=Fail");
    }
}
