# WolkConnect

Connector library written in Java for WolkAbout platform.

You can import it in your project using gradle or maven configurations below.

Gradle
------

```sh
repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    compile 'com.wolkabout:wolk:2.0.0'
}
```
Maven
-----
```sh
<repository>
  <id>jcenter</id>
  <url>https://jcenter.bintray.com/</url>
</repository>

<dependency>
  <groupId>com.wolkabout</groupId>
  <artifactId>wolk</artifactId>
  <version>2.0.0</version>
  <type>pom</type>
</dependency>
```

Example Usage
-------------
**Establishing mqtt connection with the platform:**
```sh
Device device = new Device("1017MYJAVA1");
device.setPassword("3b30742b-5d1c-4bc3-df84-c28fwfdfq59e");
device.setActuators("A1", "A2");
device.setProtocol(Protocol.JSON_SINGLE);

final Wolk wolk = Wolk.connectDevice(device)
        .toHost(Wolk.WOLK_DEMO_URL)
        .certificateAuthority(Wolk.WOLK_DEMO_CA)
        .actuationHandler((reference, value) -> {
            // TODO Invoke your code which activates your actuator. 
            System.out.println("Ref: " + reference + " value: " + value);
        })
       .actuatorStatusProvider(ref -> {
            // TODO Invoke code which reads the state of the actuator.
            return new ActuatorStatus(ActuatorStatus.Status.READY, "1");})
        .connect();
```

This will establish the connection to platform and subscribe to channels
 used for actuation commands.
 


**Publishing data:**
```sh
// add readings to the buffer
wolk.addReading("T", "25.5");
wolk.addReading("H", "40.7");

// publish readings
wolk.publish();
```

To update actuator status you can publish it like this:
```sh
wolk.publishActuatorStatus("A1");
```
This will invoke the ActuationStatusProvider to read the actuator status
 and publish it to the cloud. 

**Disconnecting from the platform:**
```sh
wolk.disconnect();