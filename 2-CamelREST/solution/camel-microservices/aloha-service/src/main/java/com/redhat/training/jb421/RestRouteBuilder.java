package com.redhat.training.jb421;

import org.springframework.stereotype.Component;
import org.apache.camel.builder.RouteBuilder;

@Component
public class RestRouteBuilder extends RouteBuilder {

	@Override
	public void configure() throws Exception {

		rest("/aloha")
			.get("{name}")
			.produces("application/json")
			.to("direct:sayHello");

		from("direct:sayHello").routeId("HelloREST")
			.setBody().simple("{\n"
			    + "  greeting: Aloha, ${header.name}\n"
			    + "  server: " + System.getenv("HOSTNAME") + "\n"
			    + "}\n");
	}

}
