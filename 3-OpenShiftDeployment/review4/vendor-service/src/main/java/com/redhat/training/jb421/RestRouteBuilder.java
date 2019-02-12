package com.redhat.training.jb421;

import org.springframework.stereotype.Component;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;

@Component
public class RestRouteBuilder extends RouteBuilder {

	@Override
	public void configure() throws Exception {

		rest("/vendor")
			.get("{vendorId}")
			.produces("application/json")
			.to("direct:getVendor");

		from("direct:getVendor").routeId("GetVendor")
			.bean(DelayBean.class, "waitDelay")
			.to("sql:select * from bookstore.Vendor where id= :#vendorId"
					+ "?dataSource=mysqlDataSource&outputType=SelectOne"
					+ "&outputClass=com.redhat.training.jb421.model.Vendor")
			.choice()
				.when(header("CamelSqlRowCount").isGreaterThan(0))
					.marshal().json(JsonLibrary.Jackson).endChoice()
				.otherwise()
					.setBody(simple("Vendor not found!"));
	}

}
