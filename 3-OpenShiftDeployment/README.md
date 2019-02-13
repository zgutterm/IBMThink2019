# Deploying Microservices with Red Hat Fuse on OpenShift

## Introduction
In this lab, you will deploy two simple example microservices, a
`catalog-service` and a `vendor-service`.

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
Now using project "review4" on server "https://api.starter-us-west-2.openshift.com:443".

You can add applications to this project with the 'new-app' command. For example, try:

    oc new-app centos/ruby-22-centos7~https://github.com/openshift/ruby-ex.git

to build a new example application in Ruby.
```

In OpenShift projects are a tool that allows a community of users to organize
and manage their content in isolation from other communities.  Technically
speaking a project is a Kubernetes namespace with additional annotations, and is
the central vehicle by which access to resources for regular users is managed.
Users must be given access to projects by administrators, or if allowed to
create projects, automatically have access to their own projects.

## Deploy a MySQL Pod
For this example, both of the microservices will use the same MySQL database as
backend storage.  In this section, you will deploy a MySQL container as a pod on
your OpenShift Online cluster, and then populate the database with some sample
data for the `CatalogItem` and `Vendor` tables.

The sample data includes 33 `CatalogItem` enrtries and 2 `Vendor` entries.

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
`MYSQL_DATABASE` environment variables supported by the container image that is
referenced by the `openshift/mysql` ImageStream.

2. Make sure your pod is running using the `oc get pods -w` command:
```sh
[student@workstation 3-OpenShiftDeployment]$ oc get pods -w
NAME            READY     STATUS    RESTARTS   AGE
mysql-1-x7vg8   1/1       Running   0          2m
```
Wait until you see the mysql pod without `build` or `deploy` in the name is in a
`STATUS` of `Running` and `Ready` lists `1/1`.
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

4. Use the `oc rsh` command to gain access to a remote shell inside the pod:
```sh
[student@workstation 3-OpenShiftDeployment]$ oc rsh mysql-1-x7vg8
sh-4.2$
```
_Note: Be sure to swap in your pod's name in the previous command_

5. Execute the script inside the pod.
```sh
sh-4.2$ mysql -ubookstore -predhat bookstore < /tmp/create-db.sql
```

## Prepare the Fabric8 Maven Plugin - `vendor-service`

The `fabric8-maven-plugin` (f8-m-p) brings your Java applications on to
Kubernetes and OpenShift. It provides a tight integration into Maven and
benefits from the build configuration already provided. This plugin focus on two
tasks: Building Docker images and creating Kubernetes and OpenShift resource
descriptors.

The two main areas that require configuration when using this plugin are:
- The `pom.xml` file, where you include the plugin dependency and configuration.
- The YAML or JSON based resource descriptor templates used to create OpenShift
or Kubernetes resources.  These are located in the `src/main/fabric8` directory
of the projects.

1. Configure the plugin in the `pom.xml` file.

Open the `pom.xml` file for the `vendor-service`. Include the
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
basic JVM environment oriented towards JAR-based deployments such as Spring
Boot.  This configuration specifically tells the Fabric8 Maven plugin to use the
`openshift/java` base image as the starting point for building the
`vendor-service` container that will get deployed on the OpenShift cluster.


2. Configure the liveness and readiness probes that OpenShift will use to
montior the microservice in the `DeploymentConfig`.

In the `vendor-service` open the `src/main/fabric8/deployment.yml` file.
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
The liveness and readiness probes are used by Kubernetes/OpenShift to determine if a pod
is healthy and ready to serve requests.  Pods that do not return an HTTP status
of `200 OK` when their probes are called by Kuberenetes will not be used to serve
requests.  Kubernetes will also attempt to kill and restart unhealthy containers
in the event that the problem is temporary and can be resolved with a simple
restart.



4. Review the `Service` definition that fabric8 will use to create a `Service`
that provides load balancing between all the pods running the `vendor-service`
container.

In the same directory, open the `service.yml` file:

```yaml
---
apiVersion: "v1"
kind: "Service"
spec:
  ports:
    - name: 8081-tcp
      protocol: TCP
      port: 8081
      targetPort: 8081
```
This `Service` tells OpenShift to access the Pods created for the
`vendor-service` container image using port `8081` as well as to listen on port
`8081`.


5. Review the `Route` definition that fabric8 will use to create a `Route` route
which provides external access to the `vendor-service` `Service` endpoint.

In the same directory, open the `route.yml` file:

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


## Deploy the `vendor-service`

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
In project review4 on server https://api.starter-us-west-2.openshift.com:443

svc/mysql - 172.30.150.30:3306
  dc/mysql deploys openshift/mysql:5.7
    deployment #1 deployed 8 hours ago - 1 pod

http://vendor-service-review4.7e14.starter-us-west-2.openshiftapps.com to pod port 8081 (svc/vendor-service)
  dc/vendor-service deploys istag/vendor-service:1.0 <-
    bc/vendor-service-s2i source builds uploaded code on openshift/java:latest
    deployment #1 running for 6 seconds - 0/1 pods

View details with 'oc describe <resource>/<name>' or list everything with 'oc get all'.
```
In this example the `vendor-service` can be reached from outside the OpenShift cluster
using the following URL:
http://vendor-service-review4.7e14.starter-us-west-2.openshiftapps.com/camel/vendor/{id}

_Note: Your personal URL will be different from the one shown above_

## Prepare the Maven Fabric8 Plugin - `catalog-service`

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
The `catalog-service` also has a `route.yml` and `service.yml` similar to the
`vendor-service` feel free to review those now, but they have been created for
you.


## Complete the `catalog-service` Camel Routes

### Update the `application.properties` file to use OCP service discovery
1. Open the `application.properties` file located inside `src/main/resources`.
Use the OCP provided environment variables to reference the host name and port
number where the `vendor-service` will be located.
```properties
vendorHost = ${VENDOR_SERVICE_SERVICE_HOST}
vendorPort = ${VENDOR_SERVICE_SERVICE_PORT}
```

### Add a connection to the `vendor-service`
Update the `RestRouteBuilder` class to invoke the `vendor-service`
microservice using the Camel HTTP4 component and the provided `vendorHost` and
`vendorPort` variables.

1. Recover the vendor id from the `catalog_vendor_id`
header set by the `SqlProcessor` processor implementation, and then set this
value in the `Exchange.HTTP_PATH` header:

```java
from("direct:getVendor")
  .removeHeader(Exchange.HTTP_URI)
   //TODO: Add the catalog_vendor_id header
  .setHeader(Exchange.HTTP_PATH,simple("${header.catalog_vendor_id}"))
  .setHeader(Exchange.HTTP_METHOD, simple("GET"))
  //TODO: Invoke the vendor-service microservice
  .to("http4:"+ vendorHost +":"+ vendorPort +"/camel/vendor")
```
The `http4` component uses the `Exchange.HTTP_PATH` header to build the URL it
will call when it sends a request to the `vendor-service`.

2. Update the `RestRouteBuilder` class to use the `http4` component to call the
`vendor-service`.  Use the `vendorHost` and `vendorPort` variables which have
been set from the values in the `application.properties` file.

```java
//TODO: Invoke the vendor-service microservice
.to("http4:"+ vendorHost +":"+ vendorPort +"/camel/vendor")
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
    .executionTimeoutInMilliseconds(3000) // 3 second timeout
    .circuitBreakerRequestVolumeThreshold(2) // 2 request minimum
    .metricsRollingPercentileWindowInMilliseconds(60000) // 60 second rolling window
    .circuitBreakerSleepWindowInMilliseconds(20000) // 20 second sleep when opened
    .circuitBreakerErrorThresholdPercentage(50) // 50 percent error threshold to open
  .end()
  //TODO: Invoke the vendor-service microservice
  .to("http4:"+ vendorHost +":"+ vendorPort +"/camel/vendor")
  .process(new VendorProcessor())
.onFallback()
  .transform(constant(VENDOR_ERROR_MSG))
.endHystrix();
```


## Deploy the `catalog-service`

Use the Fabric8 Maven Plug-in to build a container image for the
`catalog-service` Spring Boot application and create the required resources on
OpenShift.

1. Navigate to the `review4/catalog-service` folder and invoke the
`fabric8:deploy` Maven goal, enabling the `openshift` Maven profile:

```sh
[student@workstation catalog-service]$ mvn -Popenshift fabric8:deploy
```

2. Capture the hostname assigned to OpenShift Route that fronts the
`catalog-service`.  Use the `oc status` command to find the hostname:

```sh
[student@workstation catalog-service]$ oc status
In project review4 on server https://api.starter-us-west-2.openshift.com:443

svc/mysql - 172.30.150.30:3306
  dc/mysql deploys openshift/mysql:5.7
    deployment #1 deployed 8 hours ago - 1 pod

http://vendor-service-review4.7e14.starter-us-west-2.openshiftapps.com to pod port 8081 (svc/vendor-service)
  dc/vendor-service deploys istag/vendor-service:1.0 <-
    bc/vendor-service-s2i source builds uploaded code on openshift/java:latest
    deployment #1 running for 6 seconds - 0/1 pods

View details with 'oc describe <resource>/<name>' or list everything with 'oc get all'.
```
In this example the `catalog-service` can be reached from outside the OpenShift cluster
using the following URL:
http://vendor-service-review4.7e14.starter-us-west-2.openshiftapps.com/camel/vendor/{id}

_Note: Your personal URL will be different from the one shown above_


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
[student@workstation catalog-service]$ curl -si http://catalog-service-review4.7e14.starter-us-west-2.openshiftapps.com/camel/vendor/1
{"id":1,"name":"Bookmart, Inc."}

```

3. Send an HTTP `GET` to http://catalog-service-review4.7e14.starter-us-west-2.openshiftapps.com/camel/catalog/1.
This returns a `200 OK` HTTP response code. The response includes the information
about the catalog item with ID of 1:

```sh
[student@workstation catalog-service]$ curl -si http://catalog-service-review4.7e14.starter-us-west-2.openshiftapps.com/camel/catalog/1
HTTP/1.1 200 OK
...
{"id":1,"description":"description 1","author":"Lt. Howard Payson","vendorName":"Bookmart, Inc."}
```

4. Run the `oc scale` command to scale the number of pods running the
`vendor-service` microservice to `0` effectively temporarily taking it offline:

```sh
[student@workstation catalog-service]$ oc scale --replicas=0 dc vendor-service
deploymentconfig "vendor-service" scaled
```

5. Send an HTTP GET to http://catalog-service-review4.7e14.starter-us-west-2.openshiftapps.com/camel/catalog/1.
This returns a `500 Error` HTTP response code. Look at the Response body:

```sh
[student@workstation catalog-service]$ curl -si http://catalog-service-review4.7e14.starter-us-west-2.openshiftapps.com/camel/catalog/1
...
ERROR Locating Vendor
```

6. Send 3 more of the same HTTP request until you observe the circuit breaker
working and responses returning immediately instead of waiting for the requests
to timeout.

7.  Run the `oc scale` command to scale the number of pods running the
`vendor-service` microservice to `1`:

```sh
[student@workstation catalog-service]$ oc scale --replicas=1 dc vendor-service
deploymentconfig "vendor-service" scaled
```

8. Re-test sending an HTTP `GET` to http://catalog-service-review4.7e14.starter-us-west-2.openshiftapps.com/camel/catalog/1.
This now again returns a `200 OK` HTTP response code.

```sh
[student@workstation catalog-service]$ curl -si http://catalog-service-review4.7e14.starter-us-west-2.openshiftapps.com/camel/catalog/1
HTTP/1.1 200 OK
...
{"id":1,"description":"description 1","author":"Lt. Howard Payson","vendorName":"Bookmart, Inc."}
```
