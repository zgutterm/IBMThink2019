# Processing Orders with the File Component

In this exercise, you will be writing a Camel route that is capable of receiving multiple "order" files, ensuring that there are no duplicates, and then placing them into another folder using the `file` component.

## Prerequisites
Ensure that you have Maven installed.

Clone the lab repository (or download it as a ZIP):
```
  $ git clone https://github.com/zgutterm/IBMThink2019.git
```
Using your favorite IDE, import or open the `IBMThink2019/1-CreatingBasicRoutes/processing-orders` project.

If using JBoss Developer Studio, click File -> Import -> Maven -> Existing Maven Projects and click Next. Navigate to `IBMThink2019/1-CreatingBasicRoutes/processing-orders` and click Ok. It may take a few moments for Maven to download the project dependencies.

Note: Your project will import with errors. This is expected. You will resolve these errors in this exercise.

## Update the pom.xml File
Navigate to the pom.xml in the root directory of the project.

Add the `camel-core` and `camel-sprint` dependencies to the project

```<!-- TODO add camel dependencies -->
<dependency>
  <groupId>org.apache.camel</groupId>
  <artifactId>camel-core</artifactId>
</dependency>

<dependency>
  <groupId>org.apache.camel</groupId>
  <artifactId>camel-spring</artifactId>
</dependency>
```

Note that no version element is specified. This is because the version is inherited from the jboss-fuse-parent bill of materials (BOM) included the parent project pom.xml file.

Save the changes.

## Write Your Camel Route.

### Extend the FileRouteBuilder class with RouteBuilder
Open the `FileRouteBuilder` class in your IDE.

Update the class to extend `org.apache.camel.builder.RouteBuilder` and ensure that you are importing this package.

```
import org.apache.camel.builder.RouteBuilder;

//TODO: Enable the route by extending the RouteBuilder superclass
public class FileRouteBuilder extends RouteBuilder{

    //TODO Implement the configure method
}

```


### Implement the configure method

1. The superclass requires the implementation of the configure method. Add an empty method:

```
//TODO Implement the configure method
@Override
public void configure() throws Exception {

}
```

2. Add a file consumer to the route using the `file:` component.

Configure the endpoint to consume from the orders/incoming directory and use the include option to configure the endpoint to consume only XML files where the name starts with `order`:


```
public void configure() throws Exception {
  from("file:orders/incoming?include=order.*xml")
}
```

3. Add a file producer to the route using the `file:` component.

Configure the endpoint to create the outgoing files to the orders/outgoing folder. The route must throws a GenericFileOperationException exception if a duplicate file is provided in the orders/incoming folder.

```
public void configure() throws Exception {
  from("file:orders/incoming?include=order.*xml")
    .to("file:orders/outgoing?fileExist=Fail");
}
```

4. Save your changes.

### Populate the orders/incoming directory

In a terminal, navigate to the `IBMThink2019/1-CreatingBasicRoutes/processing-orders` and run the `setup-data.sh` script:

```[student@workstation processing-orders]$ ./setup-data.sh
...
'Preparation complete!'
```

Verify that the order xml files were generated in the correct folder:

```
[student@workstation processing-orders]$ ls orders/incoming
noop-1.xml  order-2.xml  order-3.xml  order-4.xml  order-5.xml  order-6.xml
```

## Run and Test Your Camel Route

### Test the Route

1. Run the route by using the camel:run Maven goal:

```
[student@workstation processing-orders]$ mvn clean camel:run
```


2. Open a new terminal window and inspect the orders/outgoing folder to verify that only order files are available:

```
[student@workstation processing-orders]$ ls orders/outgoing
order-2.xml  order-3.xml  order-4.xml  order-5.xml  order-6.xml
```

Notice that the file named `noop-1.xml` is not present.

3. Run the `./duplicate-files.sh` script in the `processing-orders` directory to recreate the files to trigger a duplicate file error.

```
[student@workstation processing-orders]$ ./duplicate-files.sh
...
'Duplication complete!'
```

4. Return to the terminal running the Camel route to see the `GenericFileOperationException` as a result of the duplicate files.

```
INFO  Received hang up - stopping the main instance.
...
INFO  Route: route1 shutdown complete, was consuming from: file://orders/incoming?include=order.*xml
INFO  Graceful shutdown of 1 routes completed in 0 seconds
...
INFO  Apache Camel 2.21.0.fuse-000077-redhat-1 (CamelContext: jb421Context) uptime 2 minutes
INFO  Apache Camel 2.21.0.fuse-000077-redhat-1 (CamelContext: jb421Context) is shutdown in 0.044 seconds
```

5. Terminate the route using `Ctrl+C` in the terminal where the route is running. If you are using JBDS/Eclipse, right click on the project and click `Close Project`.
