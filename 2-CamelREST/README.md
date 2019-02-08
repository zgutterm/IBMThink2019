# Microservies with Camel Routes

In this exercise, you will implement and execute two Camel-based microservices. The first service, the aloha service, returns a simple greeting. The second service behaves similarly, but includes an endpoint that calls the aloha service.

## Prerequisites
Ensure that you have Maven installed.

Clone the lab repository (or download it as a ZIP):
```
  $ git clone https://github.com/zgutterm/IBMThink2019.git
```
Using your favorite IDE, import or open the `IBMThink2019/2-CamelREST/camel-microservices/hola-service` project and `IBMThink2019/2-CamelREST/camel-microservices/aloha-service` project.

If using JBoss Developer Studio, click File -> Import -> Maven -> Existing Maven Projects and click Next. Navigate to `IBMThink2019/2-CamelREST/camel-microservices/hola-service` and click Ok. It may take a few moments for Maven to download the project dependencies.

Similarly, import the `IBMThink2019/2-CamelREST/camel-microservices/aloha-service` project.

The `hola-service` will have errors. You will resolve this at a later step.

## Implement the Aloha service

### Create the REST service

The Aloha service should take a single parameter so that requests to the `/aloha` service will return "Hello, {name}".

Navigate to the aloha-service/src/main/java/com/redhat/training/jb421/RestRouteBuilder.java file.

Create an endpoint at `/aloha` in the `configure()` method:

```
@Override
public void configure() throws Exception {
		//TODO implement the rest service
		rest("/aloha")

	}
```


Set the GET method with a `name` parameter:

```
@Override
public void configure() throws Exception {
		//TODO implement the rest service
		rest("/aloha")
			.get("{name}")

	}
```

Configure the service to produce `application/json`:

```
@Override
	public void configure() throws Exception {
		//TODO implement the rest service
		rest("/aloha")
			.get("{name}")
			.produces("application/json");
	}
```

### Format the Aloha Service Response

The endpoint now accepts a parameter at `/aloha`, but doesn't provide any kind of response.

The `direct` component is a simple way to connect two routes synchronously. In this instance, we can use this component to return our greeting.

In the same `configure()` method below the `rest` route, add a route `from` "sayHello" and set the name of the route as `HelloREST`.

```
    rest("/aloha")
			.get("{name}")
			.produces("application/json")

    //TODO add a direct route for printing the greeting
		from("direct:sayHello").routeId("HelloREST")
			.setBody().simple("{\n"
			    + "  greeting: Aloha, ${header.name}\n"
			    + "}\n");
```

Finally, add a `to` at the end of the REST route to connect the two routes:

```
@Override
	public void configure() throws Exception {
		//TODO implement the rest service
		rest("/aloha")
			.get("{name}")
			.produces("application/json")
			.to("direct:sayHello");
		//TODO add a direct route for printing the greeting
		from("direct:sayHello").routeId("HelloREST")
			.setBody().simple("{\n"
			    + "  greeting: Aloha, ${header.name}\n"
			    + "}\n");
	}
```

Save the file.

Now when you call the aloha-service using an HTTP GET request, the service returns with:

```
{
  greeting: Aloha, Developer
}
```

## Update the pom.xml File
To use the health check endpoints, we need to add the Spring Boot Actuator dependency. This enables the `/health` endpoint.

Navigate to the pom.xml for EACH project and add the following dependency:

```
<!--TODO Add Spring Boot Actuator Starter -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```


## Update the Hola Service to Call the Aloha Service

### Update the pom.xml file to use the `camel-http4` component.

Navigate to the pom.xml for `hola-service` and add the following dependency:

```
<!--TODO Add camel-http4 component-->
<dependency>
  <groupId>org.apache.camel</groupId>
  <artifactId>camel-http4</artifactId>
</dependency>
```

### Implement the REST service call

Open the `hola-service/src/main/java/com/redhat/training/jb421/RestRouteBuilder.java` file.

In order to reach the aloha service, you need to know the port and host name of the service is running on. Update the `alohaHost` and `alohaPort` variables by injecting the values form the `application.properties` file.

```
//TODO Inject value from configuration
@Value("${alohaHost}")
private String alohaHost;

//TODO Inject value from configuration
@Value("${alohaPort}")
private String alohaPort;
```

The hola-chained Camel route originally starts with a REST DSL endpoint. Because of this, it is necessary to modify some of the exchange headers that are set on the exchange by the REST DSL. The camel-http4 component shares a few of the headers set by the REST DSL, so we need to make sure their values are set properly for the outgoing call to the aloha-service instead of being set for the incoming REST call.

Unset the header value Exchange.HTTP_URI:

```
from("direct:callAloha")
  //TODO remove header Exchange.HTTP_URI
  .removeHeader(Exchange.HTTP_URI)
```

Set the header value Exchange.HTTP_PATH using the header.name value that was passed into the hola-chained endpoint originally to pass the name onto the aloha-service.

```
from("direct:callAloha")
  //TODO remove header Exchange.HTTP_URI
  .removeHeader(Exchange.HTTP_URI)
  //TODO set header Exchange.HTTP_PATH to the ${header.name} value
  .setHeader(Exchange.HTTP_PATH,simple("${header.name}"))
```

Finally, update the component from mock to http4 in the route's producer:

```
from("direct:callAloha")
  //TODO remove header Exchange.HTTP_URI
  .removeHeader(Exchange.HTTP_URI)
  //TODO set header Exchange.HTTP_PATH to the ${header.name} value
  .setHeader(Exchange.HTTP_PATH,simple("${header.name}"))
  //TODO use the http4 component instead of mock
  .to("http4:"+alohaHost +":"+alohaPort+"/camel/aloha");
```

## Implement a custom health check to verify database connectivity

Open the `DatabaseHealthCheck.java` file in the `hola-service` project. This class is responsible for ensuring that there is a connection to the database. This class implements the HealthIndicator interface, which the Spring Boot Actuator starter provides. The HealthIndicator interface requires a single method named health().

Note: This service doesn't actually use the database (yet) so this health check is purely demonstrating the capability of the health check, but doesn't actually apply to the health of this service.

Update the return statements to include a status of UP or DOWN depending on whether or not an exception occurred connecting to the database:

```
try {
      EntityManager entityManager = entityManagerFactory.createEntityManager();
      Query q = entityManager.createNativeQuery("select 1");
      q.getFirstResult();
      //TODO return status of UP
      return Health.up().build();
    }catch(Exception e) {
      //TODO return status of DOWN
      return Health.down(e).build();
    }
```

Save the file.

## Test the Services

### Test the Aloha Service
Run the aloha-service Spring Boot application and verify that all expected endpoints are active.

Navigate to the `camel-microservices/aloha-service` directory and run `mvn package`:

```
[student@workstation aloha-service]$ mvn package
...
[INFO] Scanning for projects...
[INFO]                                                                         
[INFO] ------------------------------------------------------------------------
[INFO] Building GE: Developing Microservices with Camel Routes - Aloha Service 1.0
[INFO] ------------------------------------------------------------------------
...
[INFO] Building jar: /home/student/JB421/labs/camel-microservices/aloha-service/target/aloha-service-1.0.jar
[INFO]
[INFO] --- spring-boot-maven-plugin:1.5.12.RELEASE:repackage (default) @ aloha-service ---
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 6.648 s
...
```

Run the Spring Boot application using the java -jar command. Leave the application running, and notice the log messages from XML route:

```
[student@workstation aloha-service]$ java -jar target/aloha-service-1.0.jar
...

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::       (v1.5.13.RELEASE)

...
16:52:57.384 [main] INFO  o.s.b.c.e.u.UndertowEmbeddedServletContainer - Undertow started on port(s) 8081 (http)
16:52:57.388 [main] INFO  c.redhat.training.jb421.Application - Started Application in 8.235 seconds (JVM running for 8.934)

...
```

Open another terminal and verify that the Spring Boot application responds to HTTP requests.

Invoke the curl command, using localhost as the host name with port 8081 and the /camel/hola/Developer resource URI:

```
[student@workstation aloha-service]$ curl -si \
    http://localhost:8081/camel/aloha/Developer
HTTP/1.1 200 OK
...
{
  greeting: Aloha, Developer
  server: workstation.lab.example.com
}
```

Verify that the health endpoint is running.

Invoke the curl command, using localhost as the host name with port 8181 and the /health resource URI:

```
[student@workstation aloha-service]$ curl -si http://localhost:8181/health
HTTP/1.1 200 OK
...
{"status":"UP"}
```


### Test the Hola Service

Run the hola-service Spring Boot application and verify that all expected endpoints are active.

Navigate to the `camel-microservices/hola-service` directory and run `mvn package`:

```
[student@workstation hola-service]$ mvn package
...
[INFO] Scanning for projects...
[INFO]                                                                         
[INFO] ------------------------------------------------------------------------
[INFO] Building GE: Developing Microservices with Camel Routes - Hola Service 1.0
[INFO] ------------------------------------------------------------------------
...
[INFO] Building jar: /home/student/JB421/labs/camel-microservices/hola-service/target/hola-service-1.0.jar
[INFO]
[INFO] --- spring-boot-maven-plugin:1.5.12.RELEASE:repackage (default) @ aloha-service ---
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 6.648 s
...
```

Run the Spring Boot application using the java -jar command. Leave the application running, and notice the log messages from XML route:

```
[student@workstation aloha-service]$ java -jar target/hola-service-1.0.jar
...

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::       (v1.5.13.RELEASE)

...
16:52:57.384 [main] INFO  o.s.b.c.e.u.UndertowEmbeddedServletContainer - Undertow started on port(s) 8081 (http)
16:52:57.388 [main] INFO  c.redhat.training.jb421.Application - Started Application in 8.235 seconds (JVM running for 8.934)

...
```

Open another terminal and verify that the Spring Boot application responds to HTTP requests.

Invoke the curl command, using localhost as the host name with port 8081 and the /camel/hola/Developer resource URI:

```
[student@workstation hola-service]$ curl -si \
    http://localhost:8082/camel/hola/Developer
HTTP/1.1 200 OK
...
{
  greeting: Hola, Developer
}
```

Verify that the hola-chained endpoint is working properly and able to call the aloha-service.

Invoke the curl command, using localhost as the host name with port 8082 and the /camel/hola-chained/Developer resource URI:

```
[student@workstation aloha-service]$ curl -si http://localhost:8082/camel/hola-chained/Developer
HTTP/1.1 200 OK
...
{
  greeting: Aloha, Developer
}
```

This means that the endpoint from the Hola service is directing to the Aloha service instead.

Test the health endpoint for the hola service.

Invoke the curl command, using localhost as the host name with port 8182 and the /health resource URI:

```
[student@workstation hola-service]$ curl -si http://localhost:8182/health
HTTP/1.1 503 Service Unavailable
...

{"status":"DOWN"}
```

Uh oh, the service is down. This is because the health check includes our custom health check to ensure a database is enabled. Since we aren't actually using this database (yet), the service still technically functions when hitting the endpoints.

## Extra Credit

Fix the service's health check! Start a mysql instance and update the camel-context.xml file to use the correct connection and credentials.


Even more extra credit, store all of the names into your mysql database. Print out data about the users in a new endpoint.
