# Deploying Microservices with Red Hat Fuse on OpenShift

The vendor-service REST service provides a single endpoint that retrieves vendor data from the bookstore database by ID. The data resides inside a MySQL database server.

The vendor-service REST endpoint takes a single argument, the vendor ID, and returns JSON data. The resource URI is `/camel/vendor/{id}`.


The catalog-service REST service provides a single endpoint that retrieves catalog item data in the bookstore database. The catalog-service REST endpoint takes a single argument, the catalog item ID. The resource URI is `/camel/catalog/{id}`.

The catalog-service REST service retrieves the vendor name from the vendor-service REST service.

## Prerequisites
Ensure that you have Maven installed.

Ensure that you have an OpenShift Online account and that your environment is available.

Clone the lab repository (or download it as a ZIP):
```
  $ git clone https://github.com/zgutterm/IBMThink2019.git
```
Using your favorite IDE, import or open the two projects in the `IBMThink2019/3-OpenShiftDeployment/review-4` project.

If using JBoss Developer Studio, click File -> Import -> Maven -> Existing Maven Projects and click Next. Navigate to `IBMThink2019/3-OpenShiftDeployment/review-4/vendor-service` and click Ok. It may take a few moments for Maven to download the project dependencies.

Similarly, import the `IBMThink2019/3-OpenShiftDeployment/review-4/catalog-service` project. This project will have errors after you import. These errors are resolved in later steps.

## Create the OpenShift Project

1. Log in to the OpenShift cluster with the `oc` tool

Open a terminal window and run the oc login command. Replace username/password/openshiftonlineurl with your credentials. If the oc login command prompts you about using insecure connections, answer y:

```
[student@workstation ~]$ oc login -u {username} -p {password} \
    https://{openshiftonlineurl}
...
Use insecure connections? (y/n): y

Login successful.
...
```

2. Create a new project called `review4-lab` in OpenShift:

```
[student@workstation ~]$ oc new-project review4-lab
```

## Prepare the Fabric8 Plugin

1. TODO -- Make image stream for fuse on openshift available -- Also check for next step to make the name correct

2. Open the `pom.xml` file for the `catalog-service`. Include the image stream name:

```
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
            <from>openshift/fuse7-java-openshift:1.1</from>
          </spring-boot>
        </config>
      </generator>
    </configuration>
    <executions>
...
```

3. In the `catalog-service` open the `src/main/fabric8/deployment.yml` file. Update the readiness probe and the liveness probe to both use path `/health` and port `8182`:

```
readinessProbe:
           failureThreshold: 3
           httpGet:
             #TODO change the readiness probe path
             path: /health
             #TODO change the readiness probe port
             port: 8182
             scheme: HTTP
           initialDelaySeconds: 30
           periodSeconds: 10
           successThreshold: 1
           timeoutSeconds: 5
         livenessProbe:
           failureThreshold: 3
           httpGet:
             #TODO change the liveness probe path
             path: /health
             #TODO change the liveness probe port
             port: 8182
             scheme: HTTP
           initialDelaySeconds: 30
           periodSeconds: 10
           successThreshold: 1
           timeoutSeconds: 5
```



4. In the same package, open the `route.yml` file and change the hostname to `catalog.apps.lab.example.com`:

```
spec:
  port:
    targetPort: 8082
  to:
    kind: Service
    name: ${project.artifactId}
  #TODO change the host name of the OpenShift route
  host: catalog.apps.lab.example.com
```


## Finish the Catalog-Service Routes
### Add a connection to the vendor service
1. Update the `RestRouteBuilder` class to invoke the `vendor-service` microservice using the Camel HTTP4 component and the provided `vendorHost` and `vendorPort` properties. Recover the vendor id from the `catalog_vendor_id` header defined on the SqlProcessor processor:

```
from("direct:getVendor")
  .removeHeader(Exchange.HTTP_URI)
   //TODO: Add the catalog_vendor_id header
  .setHeader(Exchange.HTTP_PATH,simple("${header.catalog_vendor_id}"))
  .setHeader(Exchange.HTTP_METHOD, simple("GET"))
  //TODO: Invoke the vendor-service microservice
  .to("http4:"+ vendorHost +":"+ vendorPort +"/camel/vendor")
```

2. Update the `RestRouteBuilder` class to invoke the `VendorProcessor` processor right after the HTTP4 component. This processor adds the vendor name to a Camel header:

```
//TODO: Invoke the vendor-service microservice
.to("http4:"+ vendorHost +":"+ vendorPort +"/camel/vendor")
//TODO: Invoke the VendorProcessor
.process(new VendorProcessor())
```

### Configure Hystrix Circuit Breaker in the Routes

Update the RestRouteBuilder class to add the circuit breaker pattern to the call from the catalog-service to the vendor-service. Include the following configuration for the circuit breaker:

* Any execution over three seconds must time out.

* The circuit breaker must receive a minimum of two requests before the circuit can open.

* The circuit breaker must only monitor a rolling window of the last 60 seconds of requests when deciding whether to open.

* When opened, the circuit breaker must wait 20 seconds before attempting to close again.

* The circuit breaker must open if greater than 50 percent of the requests are failing.

* If there is a failure, or the circuit is open, the message `Vendor Not Available!` must be used as a fallback.


```
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

Use the Fabric8 Maven Plug-in to build a container image for the Spring Boot application and create the required resources on OpenShift.

Navigate to the `review4/catalog-service` folder and invoke the fabric8:deploy Maven goal, from the `openshift` Maven profile:

```
[student@workstation catalog-service]$ mvn -Popenshift fabric8:deploy
```


## Test the microservices

1. Wait for the application pod to be ready and running.

Run the oc get pod command until the output resembles the following:

```
[student@workstation catalog-service]$ oc get pod
NAME                                   READY     STATUS      RESTARTS   AGE
catalog-service-1-mwvl8       1/1       Running     0          33s
catalog-service-s2i-1-build   0/1       Completed   0          58s
vendor-service-1-w62kv        1/1       Running     0          13m
vendor-service-s2i-1-build    0/1       Completed   0          13m
```

2. Send an HTTP GET to http://vendor.apps.lab.example.com/camel/vendor/1. This returns a 200 HTTP response code. Look at the Response body tab:
```
[student@workstation aloha-service]$ curl -si http://catalog.apps.lab.example.com/camel/vendor/1
{"id":1,"name":"Bookmart, Inc."}

```

3. Send an HTTP GET to http://catalog.apps.lab.example.com/camel/catalog/1. This returns a 200 HTTP response code. Look at the Response body:
```
[student@workstation aloha-service]$ curl -si http://catalog.apps.lab.example.com/camel/catalog/1
HTTP/1.1 200 OK
...
{"id":1,"description":"description 1","author":"Lt. Howard Payson","vendorName":"Bookmart, Inc."}
```

4. Run the block-vendor.sh bash script to block connections to the vendor-service microservice:

```
[student@workstation catalog-service]$ cd ..
[student@workstation review4]$ ./block-vendor.sh

```

Send an HTTP GET to http://catalog.apps.lab.example.com/camel/catalog/1. This returns a 500 HTTP response code. Look at the Response body:
```
[student@workstation aloha-service]$ curl -si http://catalog.apps.lab.example.com/camel/catalog/1
...
ERROR Locating Vendor
```

## Extra Credit

TODO
