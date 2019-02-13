# Processing Orders with the File Component

## Introduction
In this exercise, you will write a very basic Camel route that receives multiple
"order" files, ensuring that there are no duplicates, and then places them into
another folder using the `file` component.

## Prerequisites
Ensure that you have Maven installed.

Clone the lab repository (or download it as a ZIP):
```sh
  $ git clone https://github.com/zgutterm/IBMThink2019.git
```
Using your favorite IDE, import or open the `IBMThink2019/1-CreatingBasicRoutes/processing-orders` project.

If using JBoss Developer Studio, click File -> Import -> Maven -> Existing
Maven Projects and click *Next*. Navigate to
`IBMThink2019/1-CreatingBasicRoutes/processing-orders` and click *Ok*. It may
take a few moments for Maven to download the project dependencies.

_Note: Your project will import with errors. This is expected. You will resolve these errors in this exercise._

## Update the pom.xml File
First we need to include the `camel-core` and `camel-spring` Maven dependencies
in our `pom.xml` file.  This gives us access to the Camel APIs that we need to
begin creating Camel routes.

1. Navigate to the `pom.xml` in the root directory of the project.

2. Add the `camel-core` and `camel-sprint` dependencies to the project

```xml
<!-- TODO add camel dependencies -->
<dependency>
  <groupId>org.apache.camel</groupId>
  <artifactId>camel-core</artifactId>
</dependency>

<dependency>
  <groupId>org.apache.camel</groupId>
  <artifactId>camel-spring</artifactId>
</dependency>
```

_Note: No version element is specified on these dependencies. This is because_
_the version is inherited from the `jboss-fuse-parent` bill of materials (BOM)_
_included the project's `pom.xml` file._

3. Save the changes.

_Note: If you are having issues resolving the Maven dependencies you may need to_
_update your `~/.m2/settings.xml` to include the Red Hat Maven repositories._
_An example of this file can be found in the root of this Github repository._

## Write Your Camel Route.

### Make the FileRouteBuilder class extend the RouteBuilder superclass
`RouteBuilder` is a base class which is extended from to create routing rules
using the DSL. Instances of `RouteBuilder` are then added to the `CamelContext`.

1. Open the `FileRouteBuilder` class in your IDE.

2. Update the class to extend `org.apache.camel.builder.RouteBuilder` and ensure
that you are importing this package.

```java
import org.apache.camel.builder.RouteBuilder;

//TODO: Enable the route by extending the RouteBuilder superclass
public class FileRouteBuilder extends RouteBuilder{

    //TODO Implement the configure method
}

```


### Implement the configure method

1. The superclass requires the implementation of the `configure()` method.
Add an empty method:

```java
//TODO Implement the configure method
@Override
public void configure() throws Exception {

}
```

2. Add a file consumer to the route using the `file:` component.

Configure the endpoint to consume from the orders/incoming directory and use the
include option to configure the endpoint to consume only XML files where the
name starts with `order`:


```java
public void configure() throws Exception {
  from("file:orders/incoming?include=order.*xml")
}
```

3. Add a file producer to the route using the `file:` component.

Configure the endpoint to create the outgoing files to the `orders/outgoing`
folder. The route must throws a `GenericFileOperationException` exception if a
duplicate file is provided in the `orders/incoming` folder.

```java
public void configure() throws Exception {
  from("file:orders/incoming?include=order.*xml")
    .to("file:orders/outgoing?fileExist=Fail");
}
```

4. Save your changes.

### Review the camel-context configuration
In order to connect our `RouteBuilder` to the `CamelContext` it must be
configured as a bean in the Spring XML configuration file.

1.  Open the `src/main/resources/META-INF/spring/bundle-context.xml` file in
your IDE:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
       http://www.springframework.org/schema/beans/spring-beans.xsd
       http://camel.apache.org/schema/spring
       http://camel.apache.org/schema/spring/camel-spring.xsd">
    <bean class="com.redhat.training.jb421.FileRouteBuilder" id="fileRouteBuilder"/>
    <camelContext id="jb421Context" xmlns="http://camel.apache.org/schema/spring">
        <routeBuilder ref="fileRouteBuilder"/>
    </camelContext>
</beans>
```
Notice the reference to the `FileRouteBuilder` class which is assigned an ID of
`fileRouteBuilder`.  This ID is used in the `ref` attribute of the `routeBuilder`
tag found inside the `camelContext` tag.

### Populate the orders/incoming directory

**If you are using a Windows machine, you need to manually copy the files from**
**`IBMThink2019/1-CreatingBasicRoutes/orders` into a new folder**
**`IBMThink2019/1-CreatingBasicRoutes/processing-orders/orders/incoming/`.**
**Rename one of the files to noop-1.xml and move on to the next step.**

1. In a terminal window, navigate to the `IBMThink2019/1-CreatingBasicRoutes/processing-orders`
and run the `setup-data.sh` script:

```sh
[student@workstation processing-orders]$ ./setup-data.sh
...
'Preparation complete!'
```

2. Verify that the order xml files were generated in the correct folder:

```sh
[student@workstation processing-orders]$ ls orders/incoming
noop-1.xml  order-2.xml  order-3.xml  order-4.xml  order-5.xml  order-6.xml
```

## Run and Test Your Camel Route

### Test the Route

1. Run the route by using the `camel:run` Maven goal:
```sh
[student@workstation processing-orders]$ mvn clean camel:run
```

This goal uses the `camel-maven-plugin` to run your Camel Spring configurations
in a forked JVM from Maven. This makes it very easy to spin up and test your
routing rules without having to write a `main(…)` method; it also lets you
create multiple JARs to host different sets of routing rules and easily test
them independently.

The plugin compiles the source code in the Maven project,
then boots up a Spring `ApplicationContext` using the XML confiuration files
found on the classpath at `META-INF/spring/*.xml`.

2. Open a new terminal window and inspect the `orders/outgoing` folder to verify
that only order files are available:
```sh
[student@workstation processing-orders]$ ls orders/outgoing
order-2.xml  order-3.xml  order-4.xml  order-5.xml  order-6.xml
```

_Notice that the file named `noop-1.xml` is not present._

3. Run the `./duplicate-files.sh` script in the `processing-orders` directory to
recreate the files to trigger a duplicate file error.
```sh
[student@workstation processing-orders]$ ./duplicate-files.sh
...
'Duplication complete!'
```

**If you are running this exercise on a windows machine, rather than running**
**this script, copy the files from `IBMThink2019/1-CreatingBasicRoutes/orders`**
**into `IBMThink2019/1-CreatingBasicRoutes/processing-orders/orders/incoming/`**
**again.**

4. Return to the terminal running the Camel route to see the
`GenericFileOperationException` as a result of the duplicate files.
```sh
INFO  Received hang up - stopping the main instance.
...
INFO  Route: route1 shutdown complete, was consuming from: file://orders/incoming?include=order.*xml
INFO  Graceful shutdown of 1 routes completed in 0 seconds
...
INFO  Apache Camel 2.21.0.fuse-000077-redhat-1 (CamelContext: jb421Context) uptime 2 minutes
INFO  Apache Camel 2.21.0.fuse-000077-redhat-1 (CamelContext: jb421Context) is shutdown in 0.044 seconds
```

5. Terminate the route execution using `Ctrl+C` in the terminal window where the
route is running. If you are using JBDS/Eclipse, right click on the project and
click `Close Project`.

## Extra Credit

- Having your Camel route completely crash whenever there is a duplicate order
isn't a very good design. Take a look at the `file` component documentation
here: http://camel.apache.org/file2.html

- What are some better implementations?

- Update the route to safely handle duplicate orders.
