# Deploying Microservices with Red Hat Fuse on OpenShift

## Introduction
In this lab, you will deploy two simple example microservices, a
`catalog-service` and a `vendor service`.

The `vendor-service` REST service provides a single endpoint that retrieves
vendor data from the bookstore database by ID. The data resides inside a MySQL
database server.

The `vendor-service` REST endpoint takes a single argument, the vendor ID, and
returns JSON data. The resource URI is `/camel/vendor/{id}`.


The `catalog-service` REST service provides a single endpoint that retrieves
catalog item data in the bookstore database. The `catalog-service` REST endpoint
takes a single argument, the catalog item ID. The resource URI is
`/camel/catalog/{id}`.

The `catalog-service` REST service also makes a REST call to the `vendor-service`
REST service to retrieve the vendor name based on the ID that is found in the
database.

## Prerequisites
Ensure that you have Java, Maven, and an IDE installed.

Ensure that you have an OpenShift Online account and that your environment is
available.

Clone the lab repository (or download it as a ZIP):
```
  $ git clone https://github.com/zgutterm/IBMThink2019.git
```
Using your favorite IDE, import or open the two projects `catalog-service` and
`vendor-service` in the `IBMThink2019/3-OpenShiftDeployment/review-4` project.

If using JBoss Developer Studio, click File -> Import -> Maven -> Existing Maven
Projects and click Next. Navigate to
`IBMThink2019/3-OpenShiftDeployment/review-4/vendor-service` and click *Ok*.

_Note: It may take a few moments for Maven to download the project dependencies._

The `catalog-service` project *will have errors after you import*. These errors
are resolved in later steps.

## Create the OpenShift Project

1. Log in to the OpenShift cluster with the `oc` tool

Open a terminal window and run the oc login command. Replace the
`username`/`password`/`openshiftonlineurl` placeholders with the value for your
personal cluster found in the OpenShift Online UI. If the `oc login` command
prompts you about using insecure connections, answer `y`:

```sh
[student@workstation 3-OpenShiftDeployment]$ oc login -u {username} -p {password} \
    https://{openshiftonlineurl}
...
Use insecure connections? (y/n): y

Login successful.
...
```

2. Create a new project called `review4-lab` in OpenShift using
the `oc new-project` command:

```sh
[student@workstation 3-OpenShiftDeployment]$ oc new-project review4-lab
```

In OpenShift projects are a tool that allows a community of users to organize
and manage their content in isolation from other communities.  Technically
speaking a project is a Kubernetes namespace with additional annotations, and is
the central vehicle by which access to resources for regular users is managed.
Users must be given access to projects by administrators, or if allowed to
create projects, automatically have access to their own projects.

## Deploy a MySQL Pod

1. Create the MySQL pod using the following `oc new-app` command:

```sh
[student@workstation 3-OpenShiftDeployment]$ oc new-app \
    -e MYSQL_USER=bookstore \
    -e MYSQL_PASSWORD=redhat \
    -e MYSQL_DATABASE=bookstore \
    -i openshift/mysql
```

Here you create a new MySQL pod, using the `openshift/mysql` ImageStream,
which provides a base MySQL container image which is then customized when the
container starts up using the `MYSQL_USER`, `MYSQL_PASSWORD`, and
`MYSQL_DATABASE` environment variables supported by the container image
referenced by the `openshift/mysql` ImageStream.

2. Make sure your pod is running using the `oc get pods -w` command:
```sh
[student@workstation 3-OpenShiftDeployment]$ oc get pods -w
NAME            READY     STATUS    RESTARTS   AGE
mysql-1-x7vg8   1/1       Running   0          2m
```
_Note: Your pod will have a different name than the one shown in the previous example._



3. Copy the name of the pod onto the clipboard and use the `oc rsync` command to
push the database initialization script into the pod.
```sh
[student@workstation 3-OpenShiftDeployment]$ oc rsync . mysql-1-x7vg8:/tmp/ \
--exclude=* --include=create-db.sql --no-perms
sending incremental file list
create-db.sql

sent 7,393 bytes  received 35 bytes  2,971.20 bytes/sec
total size is 7,273  speedup is 0.98
```
_Note: Be sure to swap in your pod's name in the previous command_

4. Use the `oc rsh` command to run the SQL script inside the pod.
```sh
[student@workstation 3-OpenShiftDeployment]$ oc rsh mysql-1-x7vg8
sh-4.2$
```
_Note: Be sure to swap in your pod's name in the previous command_

5. Execute the script inside the pod.
```sh
sh-4.2$ mysql -ubookstore -predhat bookstore < /tmp/create-db.sql
```

## Prepare the Maven Fabric8 Plugin - Vendor Service

1. Open the `pom.xml` file for the `vendor-service`. Include the
`openshift/java` ImageStream name in the `<from>` element in the plugin
definition:

```xml
...
<plugins>
  <plugin>
    <groupId>io.fabric8</groupId>
    <artifactId>fabric8-maven-plugin</artifactId>
    <version>${fabric8.maven.plugin.version}</version>
    <configuration>
      <generator>
        <config>
          <spring-boot>
            <!-- TODO: configure the image stream name -->
            <fromMode>istag</fromMode>
            <from>openshift/java</from>
          </spring-boot>
        </config>
      </generator>
    </configuration>
    <executions>
...
```
In OpenShift an `ImageStream` represents a pointer to a specific version of a
container image.  In this example, the `openshift/java` image stream contains a
basic JVM environment oriented towards JAR-based deployments such as Spring Boot.


2. In the `vendor-service` open the `src/main/fabric8/deployment.yml` file.
Update the readiness probe and the liveness probe to both use path `/health`
and port `8181`:

```yaml
readinessProbe:
  failureThreshold: 3
  httpGet:
    #TODO change the readiness probe path to '/health'
    path: /health
    #TODO change the readiness probe port to '8181'
    port: 8181
    scheme: HTTP
  initialDelaySeconds: 30
  periodSeconds: 10
  successThreshold: 1
  timeoutSeconds: 5
livenessProbe:
  failureThreshold: 3
  httpGet:
    #TODO change the liveness probe path to '/health'
    path: /health
    #TODO change the liveness probe port to '8181'
    port: 8181
    scheme: HTTP
  initialDelaySeconds: 30
  periodSeconds: 10
  successThreshold: 1
  timeoutSeconds: 5
```
The liveness and readiness probes are used by Kubernetes to determine if a pod
is healthy and ready to serve requests.  Pods that do not return an HTTP status
of `200 OK` when their probes are called by Kuberenetes will not be used to serve
requests.  Kubernetes will also attempt to kill and restart unhealthy containers
in the event that the problem is temporary and can be resolved with a simple
restart.



4. In the same package, open the `route.yml` file:

```yaml
spec:
  port:
    targetPort: 8081
  to:
    kind: Service
    name: ${project.artifactId}
```
This `Route` tells OpenShift to expose the OpenShift service object for the
`vendor-service` application using port 8081.


## Deploy the Vendor Service

Use the Fabric8 Maven Plug-in to build a container image for the Spring Boot
application and create the required resources on OpenShift.

1. Navigate to the `review4/vendor-service` folder and invoke the
`fabric8:deploy` Maven goal, enabling the `openshift` Maven profile:

```sh
[student@workstation vendor-service]$ mvn -Popenshift fabric8:deploy
```

2. Capture the hostname assigned to OpenShift Route that fronts the
`vendor-service`.  Use the `oc status` command to find the hostname:

```sh
[student@workstation vendor-service]$ oc status
In project ibm-think on server https://api.starter-us-west-2.openshift.com:443

svc/mysql - 172.30.150.30:3306
  dc/mysql deploys openshift/mysql:5.7
    deployment #1 deployed 8 hours ago - 1 pod

http://vendor-service-review4.7e14.starter-us-west-2.openshiftapps.com to pod port 8081 (svc/vendor-service)
  dc/vendor-service-solution deploys istag/vendor-service:1.0 <-
    bc/vendor-service-s2i source builds uploaded code on openshift/java:latest
    deployment #1 running for 6 seconds - 0/1 pods

View details with 'oc describe <resource>/<name>' or list everything with 'oc get all'.
```
In this example the `vendor-service` can be reached from outside the OpenShift cluster
using the following URL:
http://vendor-service-review4.7e14.starter-us-west-2.openshiftapps.com/camel/vendor/{id}


## Prepare the Maven Fabric8 Plugin - Catalog Service

1. Open the `pom.xml` file for the `catalog-service`. Include the
`openshift/java` ImageStream name in the `<from>` element in the plugin
definition:

```xml
...
<plugins>
  <plugin>
    <groupId>io.fabric8</groupId>
    <artifactId>fabric8-maven-plugin</artifactId>
    <version>${fabric8.maven.plugin.version}</version>
    <configuration>
      <generator>
        <config>
          <spring-boot>
            <!-- TODO: configure the image stream name -->
            <fromMode>istag</fromMode>
            <from>openshift/java</from>
          </spring-boot>
        </config>
      </generator>
    </configuration>
    <executions>
...
```

2. In the `catalog-service` open the `src/main/fabric8/deployment.yml` file.
Update the readiness probe and the liveness probe to both use path `/health`
and port `8182`:

```yaml
readinessProbe:
  failureThreshold: 3
  httpGet:
    #TODO change the readiness probe path to `/health`
    path: /health
    #TODO change the readiness probe port to '8182'
    port: 8182
    scheme: HTTP
  initialDelaySeconds: 30
  periodSeconds: 10
  successThreshold: 1
  timeoutSeconds: 5
livenessProbe:
  failureThreshold: 3
  httpGet:
    #TODO change the liveness probe path to '/health'
    path: /health
    #TODO change the liveness probe port to '8182'
    port: 8182
    scheme: HTTP
  initialDelaySeconds: 30
  periodSeconds: 10
  successThreshold: 1
  timeoutSeconds: 5
```

4. In the same package, open the `route.yml` file:

```yaml
spec:
  port:
    targetPort: 8082
  to:
    kind: Service
    name: ${project.artifactId}
```
This `Route` tells OpenShift to expose the OpenShift service object for the
`catalog-service` application using port 8082.


## Finish the Catalog-Service Routes

### Update the application.properties file to use OCP service discovery
1. Open the `application.properties` file located inside `src/main/resources`.
Use the OCP provided environment variables to reference the host name and port
number where the `vendor-service` will be located.
```properties
vendorHost = ${VENDOR_SERVICE_SOLUTION_SERVICE_HOST}
vendorPort = ${VENDOR_SERVICE_SOLUTION_SERVICE_PORT}
```
In OpenShift, a `Service` object acts a load balancer to all of the `Pods`
that are running a specific container image.  In this example, the `fabric8`
Maven plugin creates a `Service` that fronts the `vendor-service` pods.  This
service also automatically creates environment variables for the host and port
of the `vendor-service`.

### Add a connection to the vendor service
1. Update the `RestRouteBuilder` class to invoke the `vendor-service`
microservice using the Camel HTTP4 component and the provided `vendorHost` and
`vendorPort` variables. Recover the vendor id from the `catalog_vendor_id`
header set by the `SqlProcessor` processor implementation:

```java
from("direct:getVendor")
  .removeHeader(Exchange.HTTP_URI)
   //TODO: Add the catalog_vendor_id header
  .setHeader(Exchange.HTTP_PATH,simple("${header.catalog_vendor_id}"))
  .setHeader(Exchange.HTTP_METHOD, simple("GET"))
  //TODO: Invoke the vendor-service microservice
  .to("http4:"+ vendorHost +":"+ vendorPort +"/camel/vendor")
```

2. Update the `RestRouteBuilder` class to invoke the `VendorProcessor` processor
right after the HTTP4 component. This processor adds the vendor name to a Camel
header:

```java
//TODO: Invoke the vendor-service microservice
.to("http4:"+ vendorHost +":"+ vendorPort +"/camel/vendor")
//TODO: Invoke the VendorProcessor
.process(new VendorProcessor())
```

### Configure Hystrix Circuit Breaker in the Routes

Update the `RestRouteBuilder` class to add the circuit breaker pattern to the
call from the `catalog-service` to the `vendor-service`. Include the following
configuration specifications for the circuit breaker:

* Any execution over three seconds must time out.
* The circuit breaker must receive a minimum of two requests before the circuit
can open.
* The circuit breaker must only monitor a rolling window of the last 60 seconds
of requests when deciding whether to open.
* When opened, the circuit breaker must wait 20 seconds before attempting to
close again.
* The circuit breaker must open if greater than 50 percent of the requests are
failing.
* If there is a failure, or the circuit is open, the message
`Vendor Not Available!` must be used as a fallback.

```java
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
```


## Deploy the Catalog Service

Use the Fabric8 Maven Plug-in to build a container image for the Spring Boot
application and create the required resources on OpenShift.

1. Navigate to the `review4/catalog-service` folder and invoke the
`fabric8:deploy` Maven goal, enabling the `openshift` Maven profile:

```sh
[student@workstation catalog-service]$ mvn -Popenshift fabric8:deploy
```


## Test the microservices

1. Wait for the application pod to be ready and running.

Run the oc get pod command until the output resembles the following:

```sh
[student@workstation catalog-service]$ oc get pod
NAME                                   READY     STATUS      RESTARTS   AGE
catalog-service-1-mwvl8       1/1       Running     0          33s
catalog-service-s2i-1-build   0/1       Completed   0          58s
vendor-service-1-w62kv        1/1       Running     0          13m
vendor-service-s2i-1-build    0/1       Completed   0          13m
```

2. Send an HTTP GET to http://vendor-service-review4.7e14.starter-us-west-2.openshiftapps.com/camel/vendor/1.
This returns a `200 OK` HTTP response code. The response includes the JSON data
about the vendor with ID of 1:

```sh
[student@workstation aloha-service]$ curl -si http://catalog-service-review4.7e14.starter-us-west-2.openshiftapps.com/camel/vendor/1
{"id":1,"name":"Bookmart, Inc."}

```

3. Send an HTTP `GET` to http://catalog-service-review4.7e14.starter-us-west-2.openshiftapps.com/camel/catalog/1.
This returns a `200 OK` HTTP response code. The response includes the information
about the catalog item with ID of 1:

```sh
[student@workstation aloha-service]$ curl -si http://catalog-service-review4.7e14.starter-us-west-2.openshiftapps.com/camel/catalog/1
HTTP/1.1 200 OK
...
{"id":1,"description":"description 1","author":"Lt. Howard Payson","vendorName":"Bookmart, Inc."}
```

4. Run the `oc delete` command to block connections to the
`vendor-service` microservice:

```sh
[student@workstation catalog-service]$ oc delete svc vendor-service-solution

```

Send an HTTP GET to http://catalog-service-review4.7e14.starter-us-west-2.openshiftapps.com/camel/catalog/1. This
returns a 500 HTTP response code. Look at the Response body:

```sh
[student@workstation catalog-service]$ curl -si http://catalog-service-review4.7e14.starter-us-west-2.openshiftapps.com/camel/catalog/1
...
ERROR Locating Vendor
```
