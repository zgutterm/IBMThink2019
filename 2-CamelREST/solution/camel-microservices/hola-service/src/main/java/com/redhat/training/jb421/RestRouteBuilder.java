package com.redhat.training.jb421;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RestRouteBuilder extends RouteBuilder {

	@Value("${alohaHost}")
	private String alohaHost;
	
	@Value("${alohaPort}")
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
			.removeHeader(Exchange.HTTP_URI)
			.setHeader(Exchange.HTTP_PATH,simple("${header.name}"))
			.to("http4:"+alohaHost +":"+alohaPort+"/camel/aloha");

		from("direct:sayHello").routeId("HelloREST")
			.setBody().simple("{\n"
			    + "  greeting: Hola, ${header.name}\n"
			    + "  server: " + System.getenv("HOSTNAME") + "\n"
			    + "}\n");
	}

}
