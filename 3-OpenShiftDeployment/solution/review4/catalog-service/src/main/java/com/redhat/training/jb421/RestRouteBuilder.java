package com.redhat.training.jb421;

import com.redhat.training.jb421.processor.ResponseProcessor;
import com.redhat.training.jb421.processor.SqlProcessor;
import com.redhat.training.jb421.processor.VendorProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RestRouteBuilder extends RouteBuilder {

	@Value("${vendorHost}")
	private String vendorHost;

	@Value("${vendorPort}")
	private String vendorPort;

	private static final String VENDOR_ERROR_MSG = "Vendor Not Available!";

	@Override
	public void configure() throws Exception {

		rest("/catalog/")
			.get("{catalogId}")
				.produces("application/json")
				.to("direct:getCatalog");


		from("direct:getCatalog")
				.to("sql:select id, author, description, vendor_id as vendorId from bookstore.CatalogItem where id=:#catalogId"
							+ "?dataSource=mysqlDataSource&outputType=SelectOne"
							+ "&outputClass=com.redhat.training.jb421.model.CatalogItem")

				//TODO: invoke the SqlProcessor
				.process(new SqlProcessor())
				.to("direct:getVendor")
				.choice()
					.when(body().isEqualTo(VENDOR_ERROR_MSG))
						.setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
						.transform(constant("ERROR Locating Vendor"))
					.otherwise()
						//TODO: invoke the ResponseProcessor
						.process(new ResponseProcessor())
						.marshal().json(JsonLibrary.Jackson)

				.end();



		from("direct:getVendor")
			.removeHeader(Exchange.HTTP_URI)
			//TODO: Add the catalog_vendor_id header
			.setHeader(Exchange.HTTP_PATH,simple("${header.catalog_vendor_id}"))
			.setHeader(Exchange.HTTP_METHOD, simple("GET"))
			//TODO: Add circuit breaker pattern
			.hystrix()
				.hystrixConfiguration()
			 		.executionTimeoutInMilliseconds(3000)
			 		.circuitBreakerRequestVolumeThreshold(2)
			 		.metricsRollingPercentileWindowInMilliseconds(60000)
			 		.circuitBreakerSleepWindowInMilliseconds(20000)
			 		.circuitBreakerErrorThresholdPercentage(50)
			 	.end()
				//TODO: Invoke the vendor-service microservice
			 	.to("http4:"+ vendorHost +":"+ vendorPort +"/camel/vendor")
				//TODO: Invoke the VendorProcessor
				.process(new VendorProcessor())
			.onFallback()
				.transform(constant(VENDOR_ERROR_MSG))
			.endHystrix();

	}

}
