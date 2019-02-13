# Microservices with Camel Routes

## Introduction
In this exercise, you will implement and execute two Camel-based microservices
using the Camel REST DSL. The first service, the `aloha-service`, returns a
simple greeting. The second service `hola-service` behaves similarly, but
includes a second endpoint that chains a call to the `aloha-service`.

## Prerequisites

Ensure that you have Maven installed.

Clone the lab repository (or download it as a ZIP):
```sh
$ git clone https://github.com/zgutterm/IBMThink2019.git
```

Using your favorite IDE, import or open the
`IBMThink2019/2-CamelREST/camel-microservices/hola-service` project and
`IBMThink2019/2-CamelREST/camel-microservices/aloha-service` project.

If using JBoss Developer Studio, click File -> Import -> Maven -> Existing
Maven Projects and click Next. Navigate to
`IBMThink2019/2-CamelREST/camel-microservices/hola-service` and click *Ok*.

_Note: It may take a few moments for Maven to download the project dependencies._

Similarly, import the `IBMThink2019/2-CamelREST/camel-microservices/aloha-service`
project.

_Note: The `hola-service` will have errors. You will resolve these in a later step._

## Implement the Aloha service

### Create the REST service

The Aloha service must take a single input parameter of a name so that requests to the
`/aloha` service will return `"Aloha, {name}"`.

1. Open the `aloha-service/src/main/java/com/redhat/training/jb421/RestRouteBuilder.java`
file.

2.  Create an endpoint at `/aloha` in the `configure()` method:

```java
@Override
public void configure() throws Exception {
    //TODO implement the rest service
    rest("/aloha")

}
```

3.  Set the GET method with a `name` parameter:

```java
@Override
public void configure() throws Exception {
  //TODO implement the rest service
  rest("/aloha")
    .get("{name}")

}
```

4.  Configure the service to produce `application/json`:

```java
@Override
public void configure() throws Exception {
  //TODO implement the rest service
  rest("/aloha")
    .get("{name}")
      .produces("application/json");
}
```

### Format the Aloha Service Response

The endpoint now accepts a parameter at `/aloha`, but doesn't provide any
kind of response.

The `direct` component is a simple way to connect two routes synchronously.
You can think of `direct` routes as similar to a method or sub-routine. In this
instance, we can use this component to return our greeting.

1.  In the same `configure()` method below the `rest` route, add a route
`from("direct:sayHello")` and set the name of the route as `HelloREST`.

```java
rest("/aloha")
  .get("{name}")
    .produces("application/json")

//TODO add a direct route for printing the greeting
from("direct:sayHello").routeId("HelloREST")
  .setBody().simple("{\n"
      + "  greeting: Aloha, ${header.name}\n"
      + "}\n");
```

2.  Add a `to("direct:sayHello")` at the end of the REST route to
connect the two routes:

```java
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

3.  Save the file.

Now when you call the `aloha-service` using an HTTP `GET` request, and the name
"Developer", the service returns the following response:

```json
{
  greeting: Aloha, Developer
}
```

## Add a `/health` health check endpoint using Spring Boot Actuator
The `/health` endpoint is used to check the health or state of the running
application. It is usually exercised by monitoring software to alert us if the
running instance goes down or gets unhealthy for other reasons such as
connectivity issues with our database, or lack of disk space.

### Update the `pom.xml` file
To use the health check endpoints, we need update our `pom.xml` to add the
Spring Boot Actuator dependency. This library automatically enables the
`/health` endpoint, and can potentially offer other monitoring endpoints.

1.  Navigate to the `pom.xml` for both of the projects and add the following dependency:

```xml
<!--TODO Add Spring Boot Actuator Starter -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

2. Save the file.

### Implement a custom health check to verify database connectivity

In addition to basic `/health` endpoints, the Spring Boot actuator supports
creating custom health checks.  A custom health check can collect any type of
custom health data specific to the application and automatically expose it
through the `/health` endpoint:

1. Open the `DatabaseHealthCheck.java` file in the `hola-service` project. This
class is responsible for ensuring that there is a connection to the database.
This class implements the `HealthIndicator` interface, which the Spring Boot
Actuator starter provides. The `HealthIndicator` interface requires a single
method named `health()`.

_Note: This service doesn't actually use the database (yet) so this health check_
_is purely demonstrating the capability of creating a custom health check, but_
_does **not** actually apply to the health of this service._

2. Update the return statements to include a status of `UP` or `DOWN` depending on
whether or not an exception occurred connecting to the database:

```java
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
3. Save the file.

## Update the Hola Service to Call the Aloha Service
In order to support one microservice calling the other, we need an HTTP client
implementation to make the HTTP request call.  In camel this is provided by the
`http4` component.  This component provides HTTP based endpoints for calling
external HTTP resources.  It leverages the Apache `HttpClient` to make these
calls.

### Update the pom.xml file to use the `camel-http4` component.

Navigate to the pom.xml for `hola-service` and add the following dependency:

```xml
<!--TODO Add camel-http4 component-->
<dependency>
  <groupId>org.apache.camel</groupId>
  <artifactId>camel-http4</artifactId>
</dependency>
```

### Implement the REST service call

1.  Open the `hola-service/src/main/java/com/redhat/training/jb421/RestRouteBuilder.java` file.

In order to reach the aloha service, you need to know the port and host name of
the service is running on.

2.  Update the `alohaHost` and `alohaPort` variables by injecting the values
from the `application.properties` file.

```java
//TODO Inject value from configuration
@Value("${alohaHost}")
private String alohaHost;

//TODO Inject value from configuration
@Value("${alohaPort}")
private String alohaPort;
```

The `hola-chained` Camel route originally starts with a REST DSL endpoint.
Because of this, it is necessary to modify some of the exchange headers that are
set on the exchange by the REST DSL. The `camel-http4` component shares a few
of the header values set by the REST DSL, so we need to make sure their values
are set properly for the outgoing call to the `aloha-service` instead of being
set for the incoming REST call.

1. Unset the header value `Exchange.HTTP_URI`:

```java
from("direct:callAloha")
  //TODO remove header Exchange.HTTP_URI
  .removeHeader(Exchange.HTTP_URI)
```

2. Set the header value `Exchange.HTTP_PATH` using the `header.name` value that
was passed into the `hola-chained` endpoint originally to forward the name on
to the `aloha-service`.

```java
from("direct:callAloha")
  //TODO remove header Exchange.HTTP_URI
  .removeHeader(Exchange.HTTP_URI)
  //TODO set header Exchange.HTTP_PATH to the ${header.name} value
  .setHeader(Exchange.HTTP_PATH,simple("${header.name}"))
```

3.  Finally, update the component from `mock` to `http4` in the route's producer:

```java
from("direct:callAloha")
  //TODO remove header Exchange.HTTP_URI
  .removeHeader(Exchange.HTTP_URI)
  //TODO set header Exchange.HTTP_PATH to the ${header.name} value
  .setHeader(Exchange.HTTP_PATH,simple("${header.name}"))
  //TODO use the http4 component instead of mock
  .to("http4:"+alohaHost +":"+alohaPort+"/camel/aloha");
```

## Test the Services

### Test the Aloha Service

Run the `aloha-service` Spring Boot application and verify that all expected
endpoints are active.

1. Navigate to the `camel-microservices/aloha-service` directory and run `mvn package`:

```sh
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

2. Run the Spring Boot application using the java -jar command. Leave the
application running, and notice the log messages from XML route:

```
[student@workstation aloha-service]$ java -jar target/aloha-service-1.0.jar
...
16:52:57.384 [main] INFO  o.s.b.c.e.u.UndertowEmbeddedServletContainer - Undertow started on port(s) 8081 (http)
16:52:57.388 [main] INFO  c.redhat.training.jb421.Application - Started Application in 8.235 seconds (JVM running for 8.934
```

3. Open another terminal window to verify that the Spring Boot application responds to HTTP requests.

4. Invoke the curl command, using `localhost` as the host name with port `8081` and the `/camel/hola/Developer` resource URI:

```sh
[student@workstation aloha-service]$ curl -si http://localhost:8081/camel/aloha/Developer
HTTP/1.1 200 OK
...
{
  greeting: Aloha, Developer
  server: workstation.lab.example.com
}
```

5. Verify that the health endpoint is running.  Invoke the curl command, using `localhost` as the host name with port `8181` and the `/health` resource URI:

```sh
[student@workstation aloha-service]$ curl -si http://localhost:8181/health
HTTP/1.1 200 OK
...
{"status":"UP"}
```

### Test the Hola Service

Run the `hola-service` Spring Boot application and verify that all expected endpoints are active.

1. Navigate to the `camel-microservices/hola-service` directory and run `mvn package`:

```sh
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

2. Run the Spring Boot application using the `java -jar` command. Leave the
application running, and notice the log messages from XML route:

```sh
[student@workstation aloha-service]$ java -jar target/hola-service-1.0.jar
...
 :: Spring Boot ::       (v1.5.13.RELEASE)
...
16:52:57.384 [main] INFO  o.s.b.c.e.u.UndertowEmbeddedServletContainer - Undertow started on port(s) 8081 (http)
16:52:57.388 [main] INFO  c.redhat.training.jb421.Application - Started Application in 8.235 seconds (JVM running for 8.934)
...
```

3. Open another terminal window to verify that the Spring Boot application
responds to HTTP requests.

4. Invoke the `curl` command, using `localhost` as the host name with port
`8081` and the `/camel/hola/Developer` resource URI:

```sh
[student@workstation hola-service]$ curl -si \
    http://localhost:8082/camel/hola/Developer
HTTP/1.1 200 OK
...
{
  greeting: Hola, Developer
}
```

5. Verify that the `hola-chained` endpoint is working properly and able to call
the `aloha-service`.  Invoke the `curl` command, using `localhost` as the host
name with port `8082` and the `/camel/hola-chained/Developer` resource URI:

```sh
[student@workstation aloha-service]$ curl -si http://localhost:8082/camel/hola-chained/Developer
HTTP/1.1 200 OK
...
{
  greeting: Aloha, Developer
}
```

This means that the endpoint from the Hola service is directing to the Aloha
service instead.

6. Test the health endpoint for the `hola-service`.

Invoke the `curl` command, using `localhost` as the host name with port `8182`
and the `/health` resource URI:

```sh
[student@workstation hola-service]$ curl -si http://localhost:8182/health
HTTP/1.1 503 Service Unavailable
...
{"status":"DOWN"}
```
Uh oh, the service is down. This is because the health check includes our custom
health check to ensure a database is enabled. Since we aren't actually using
this database (yet), the service still technically functions when hitting the
endpoints.

## Extra Credit

Fix the service's health check! Start a MySQL database instance and update the
`camel-context.xml` file to use the correct connection and credentials.

Even more extra credit, store all of the names into your MySQL database.
Print out data about the users in a new endpoint.
