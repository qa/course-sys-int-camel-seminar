# Camel Seminar

Camel seminar for university course: System Integration with JBoss.

## Environment

Please do the following steps to properly set your environment for this seminar.

### Directory Structure

```bash
# create directory for this seminar:
mkdir camel-seminar
cd camel-seminar

# put JBoss Fuse 6.2.0 GA to this directory
cp _PATH_/jboss-fuse-full-6.2.0.redhat-133.zip ./

# unzip JBoss Fuse 6.2.0 GA
unzip jboss-fuse-full-6.2.0.redhat-133.zip

# clone systems which will be integrated: https://github.com/qa/course-sys-int-systems
# you will need to start / restart these systems during seminar repeatedly, see Readme
git clone https://github.com/qa/course-sys-int-systems.git

# clone seminar sources: https://github.com/qa/course-sys-int-camel-seminar
git clone https://github.com/qa/course-sys-int-camel-seminar.git
```

Your directory structure should then be:

```bash
camel-seminar
  |-- course-sys-int-camel-seminar
  |-- course-sys-int-systems
  |-- jboss-fuse-6.2.0.redhat-133
  +-- jboss-fuse-full-6.2.0.redhat-133.zip
```

### JBoss Fuse 6.2.0 GA Configuration

```bash
cd jboss-fuse-6.2.0.redhat-133
```

Add users to `etc/users.properties`

* Uncomment `admin=admin,admin,...` user
* Add new user `shipuser=shippwd,admin`

Configure ActiveMQ `etc/activemq.xml`

* Add `openwire+ssl` transportConnector, the resulting element will be:

```bash
<transportConnectors>
    <transportConnector name="openwire" uri="tcp://${bindAddress}:${bindPort}"/>
    <transportConnector name="openwire+ssl" uri="ssl://${bindAddress}:61617?maximumConnections=1000"/>
</transportConnectors>
```

* Set SSL context:

```bash
<sslContext>
    <sslContext keyStore="file:${karaf.base}/bin/keystore.jks"
                keyStorePassword="redhat"
               trustStore="file:${karaf.base}/bin/keystore.jks"
                trustStorePassword="redhat"/>
</sslContext>
```

You can see the final activemq.xml in `camel-seminar/course-sys-int-camel-seminar/src/test/resources/activemq.xml`.

### IDE

To force your IDE to format properly java camel routes, set:

* Indentation by spaces (4 spaces)
* Continuation content to 4 (default would be 8)

## Running

### JBoss Fuse

```bash
cd jboss-fuse-6.2.0.redhat-133
bin/fuse
```

### System To Be Integrated

https://github.com/qa/course-sys-int-systems

```bash
cd course-sys-int-systems

mvn clean camel:run
```

Note you will need to restart these systems to reset their state during tasks development.

### Camel Seminar :: Integration Middleware

This section describes how to run / test the integration middleware developed during seminar.

```bash
cd course-sys-int-camel-seminar
```

#### Standalone & Unit Tests

```bash
# run as standalone app
mvn clean camel:run

# unit tests
mvn clean test

# single test execution
mvn clean test -Dtest=_NAME_
```

#### Integration Tests with Pax Exam

First install bundle into local maven repository, without tests:

```bash
mvn clean install -DskipTests -P bundle
```

Once you have test bundle in local maven repo, you can run integration tests.

```bash
mvn clean integration-test -P bundle
```

The first install without test is kind of hack. It's because of this pax configuration entry:

```java
bundle("mvn:com.redhat.brq.integration/camel-seminar/1.0.0-SNAPSHOT"),
```

You need to create bundle with your camel route, and deploy this bundle into fuse.
It can be achieved by creating bundle during test executin, like [this example](https://github.com/ANierbeck/Camel-Pax-Exam-Demo/blob/master/route-control/src/test/java/de/nierbeck/camel/exam/demo/control/route/KarafRoutingTest.java),
or by deploying bundle from `mvn://` or `file://`.

In case the tested route is single blueprint.xml, it could be deployed like this:

```java
bundle("file://absolute/path/to/blueprint.xml")
```

PaxExam creates features.xml, adds it into featureurls (can be verified with `features:listurl`).  
Generated xml can be found in `target/unpacked-fuse/<some hash>/test-dependencies.xml`.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<features xmlns="http://karaf.apache.org/xmlns/features/v1.0.0" name="test-dependencies">
	<feature name="test-dependencies">
		<bundle>mvn:com.redhat.brq.integration/camel-seminar/1.0.0-SNAPSHOT</bundle>
	</feature>
</features>
```

#### Deploy to JBoss Fuse

Build as bundle:

```bash
mvn clean install -DskipTests -P bundle
```

Switch to JBoss Fuse console.

You need to install some features before deployment:

```bash
features:install camel-http4
features:install camel-jackson
features:install camel-restlet
```

Then you need to set system property with path to file inbox/outbox folders:

```bash
system-property endpoint.file.baseUrl _PATH_/camel-seminar/course-sys-int-systems/target
```

Now you can install bundle from maven:

```bash
osgi:install -s mvn:com.redhat.brq.integration/camel-seminar/1.0.0-SNAPSHOT
```
## JSON object / HTTP requests to REST API

```javascript
{
    "address" : {
        "firstName" : "Jiri",
        "lastName" : "Novak",
        "street" : "Purkynova",
        "city" : "Brno",
        "zipCode" : "61200"
    },
    "items" : [
        {
            "articleId" : "6",
            "count" : "1",
            "unitPrice" : "4.4"
        }
    ]
}
```

Change `count` of article to produce inventory error.

Change `unitPrice` to value > 10000 to produce accounting error.

Change `city` to 'Gotham' to produce shipment error.

### Example curl commands

POST:

```bash
curl -X POST "localhost:8080/orders" --data '{"address":{"firstName":"Jiri","lastName":"Novak","street":"Purkynova","city":"Brno","zipCode":"61200"},"items":[{"articleId":"6","count":"1","unitPrice":"4.4"}]}' -v --header "ContentType:application/json" -v
```

GET:

```bash
curl -X GET "localhost:8080/orders/1" -v
```

## Tasks

Detailed description with steps and hints of each task is described in `OrderProcessRoute` class.

### 1] Implement new-order route

branch: `camel-00`

This route saves new Order to repository, sets response headers and returns.

### 2] Implement find-order route.

branch: `camel-01`

This route tries to find Order in repository.

### 3] Implement issue-order route.

branch: `camel-02`

This route set status of order to 'in process' and sends request to the Inventory system.

### 4] Implement receive-inventory route.

branch: `camel-03`

This route receives response from the Inventory system and handles when item cannot be reserved.

### 5] Handle the ItemNotReservedException

branch: `camel-04`

Handle the `ItemNotReservedException` thrown in *TASK 4*. Write test to verify exception handling.

### 6] Implement accounting route.

branch: `camel-05`

This route sends an order to Accounting systems and handles response.

### 7] Handle the InvalidAccountingException

branch: `camel-06`

Handle the `InvalidAccountingException` thrown in *TASK 6*. Write test to verify exception handling.

### 8] Implement shipment route

branch: `camel-07`

This route sends an address of a order to the Shipment system.

### 9] Finish ReceiveShipmentRouteTest

branch: `camel-08`

Finish `ReceiveShipmentRouteTest` for `receive-shipment` route, see hints in the test class.

### 10] Finish integration test with PaxExam

branch: `camel-09`

Finish `IntegrationTest` class. How to run integration test is described in *Running* section.

Result is in branch `camel-10`.
