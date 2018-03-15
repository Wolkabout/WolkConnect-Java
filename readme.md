# WolkConnect

Connector library written in Java for WolkAbout platform.

You can import it in your project using gradle or maven configurations below.

Prerequisite
------

Include WolkAbout Java connector to project.

Gradle

```sh
repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    compile 'com.wolkabout:wolk:2.3.0'
}
```

Maven

```sh
<repository>
  <id>jcenter</id>
  <url>https://jcenter.bintray.com/</url>
</repository>

<dependency>
  <groupId>com.wolkabout</groupId>
  <artifactId>wolk</artifactId>
  <version>2.3.0</version>
  <type>pom</type>
</dependency>
```

Example Usage
-------------
**Establishing mqtt connection with the platform:**
```java
Device device = new Device("device_key");
device.setPassword("some_password");
device.setActuators("SL", "SW");
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
            return new ActuatorStatus(ActuatorStatus.Status.READY, "1");
        })
        .configurationHandler((configuration) -> {
            // TODO Set device configuration according to received configuration
        })
        .configurationProvider(() -> {
            // TODO Read configuration from device and return it
            return new HashMap<String, String>();
        })
       .connect();
```

This will establish the connection to platform and subscribe to channels
 used for actuation commands.
 

**Publishing data:**
```java
// add readings to the buffer
wolk.addSensorReading("T", "25.6");
wolk.addSensorReading("P", "1024");
wolk.addSensorReading("H", "52");

// publish readings
wolk.publish();
```

Publish device configuration to platform:
```java
wolk.publishConfiguration()
```

Publish actuatorstatus to platform:
```java
wolk.publishActuatorStatus("SW");
wolk.publishActuatorStatus("SL");
```
This will invoke the ActuationStatusProvider to read the actuator status
 and publish it to the cloud. 

**Disconnecting from the platform:**
```java
wolk.disconnect();
```


**Data persistence:**

WolkAbout Java Connector provides mechanism for persisting data in situations where it can not be sent to WolkAbout IoT platform.
Persisted data is sent to WolkAbout IoT platform automatically once connection is established.

By default in-memory data persistence is used, in cases when this is suboptimal one can implement Persistence interface, and pass
it to Wolk via WolkBuilder in following manner:

```java
Device device = new Device("device_key");
device.setPassword("some_password");
device.setActuators("SW", "SL");
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
       .withPersistence(new LevelDBPersistence()) // Enable data persistence via custom persist mechanism
       .connect();
```

For more info on persistence mechanism see WolkBuilder.withPersistence method, and Persistence interface.

**Firmware Update:**

WolkAbout Java Connector provides mechanism for updating device firmware.

By default this feature is disabled.
See code snippet below on how to enable device firmware update.

```java
Device device = new Device("device_key");
device.setPassword("some_password");
device.setProtocol(Protocol.JSON_SINGLE);

class FirmwareUpdater implements FirmwareUpdateHandler {
    @Override
    public void updateFirmwareWithFile(Path firmwareFile) {
        // Mock install
        LOG.info("Updating firmware with file '{}'", firmwareFile);

        // Optionally delete 'firmwareFile'
    }
}


final Wolk wolk = Wolk.connectDevice(device)
        .toHost(Wolk.WOLK_DEMO_URL)
        .certificateAuthority(Wolk.WOLK_DEMO_CA)
        // Enable firmware update
        .withFirmwareUpdate("1.2.3",                             // Current firmware version                             
                            new FirmwareUpdater(),               // Implementation of FirmwareUpdateHandler, which performs installation of obtained device firmware
                            new File("./download").toPath(),     // Directory where downloaded device firmware files will be stored 
                            1024 * 1024 * 1024,                  // Maximum acceptable size of firmware file, in bytes
                            null)                                // Optional implementation of FirmwareDownloadHandler for cases when one wants to download device firmware via given URL
        .connect();
```
