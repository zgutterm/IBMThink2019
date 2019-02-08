package com.redhat.training.jb421;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RestRouteBuilder extends RouteBuilder {

	//TODO Inject value from configuration

	private String alohaHost;

	//TODO Inject value from configuration

	private String alohaPort;

	@Override
	public void configure() throws Exception {

		rest("/hola")
			.get("{name}")
			.produces("application/json")
			.to("direct:sayHello");

		rest("/hola-chained")
			.get("{name}")
			.produces("application/json")
			.to("direct:callAloha");

		from("direct:callAloha")
			//TODO remove header Exchange.HTTP_URI

			//TODO set header Exchange.HTTP_PATH to the ${header.name} value

			//TODO use the http4 component instead of mock
			.to("mock:"+alohaHost +":"+alohaPort+"/camel/aloha");

		from("direct:sayHello").routeId("HelloREST")
			.setBody().simple("{\n"
			    + "  greeting: Hola, ${header.name}\n"
			    + "  server: " + System.getenv("HOSTNAME") + "\n"
			    + "}\n");
	}

}
