```

██╗    ██╗ ██████╗ ██╗     ██╗  ██╗ ██████╗ ██████╗ ███╗   ██╗███╗   ██╗███████╗ ██████╗████████╗
██║    ██║██╔═══██╗██║     ██║ ██╔╝██╔════╝██╔═══██╗████╗  ██║████╗  ██║██╔════╝██╔════╝╚══██╔══╝
██║ █╗ ██║██║   ██║██║     █████╔╝ ██║     ██║   ██║██╔██╗ ██║██╔██╗ ██║█████╗  ██║        ██║   
██║███╗██║██║   ██║██║     ██╔═██╗ ██║     ██║   ██║██║╚██╗██║██║╚██╗██║██╔══╝  ██║        ██║   
╚███╔███╔╝╚██████╔╝███████╗██║  ██╗╚██████╗╚██████╔╝██║ ╚████║██║ ╚████║███████╗╚██████╗   ██║   
 ╚══╝╚══╝  ╚═════╝ ╚══════╝╚═╝  ╚═╝ ╚═════╝ ╚═════╝ ╚═╝  ╚═══╝╚═╝  ╚═══╝╚══════╝ ╚═════╝   ╚═╝   
                                                                                                 
                                                                     ██╗ █████╗ ██╗   ██╗ █████╗ 
                                                                     ██║██╔══██╗██║   ██║██╔══██╗
                                                          █████╗     ██║███████║██║   ██║███████║
                                                          ╚════╝██   ██║██╔══██║╚██╗ ██╔╝██╔══██║
                                                                ╚█████╔╝██║  ██║ ╚████╔╝ ██║  ██║
                                                                 ╚════╝ ╚═╝  ╚═╝  ╚═══╝  ╚═╝  ╚═╝
                                                                                                 
 
```
----

Connector library written in Java for WolkAbout platform.

You can import it in your project using gradle or maven configurations below.

Prerequisite
------

Include WolkAbout Java connector to project.

Gradle

```groovy
repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    compile 'com.wolkabout:wolk:3.0.0'
}
```

Maven

```xml
<repository>
  <id>jcenter</id>
  <url>https://jcenter.bintray.com/</url>
</repository>

<dependency>
  <groupId>com.wolkabout</groupId>
  <artifactId>wolk</artifactId>
  <version>3.0.0</version>
  <type>pom</type>
</dependency>
```

Example Usage
-------------

Create a device on WolkAbout IoT platform by importing template file [simple-example-manifest.json](https://github.com/Wolkabout/WolkConnect-Java-/blob/master/src/main/resources/simple-example-manifest.json).  
This template fits simple example and demonstrates the sending of a temperature sensor reading.  
After creating the device on the Platform, copy the provided credentials into `deviceKey` and `password`.

**Establishing mqtt connection with the platform:**
```java
final Wolk wolk = Wolk.builder()
    .mqtt()
        .host("ssl://api-demo.wolkabout.com:8883")
        .sslCertification("ca.crt")
        .deviceKey("devicekey")
        .password("password")
        .build()
    .protocol(ProtocolType.JSON_SINGLE_REFERENCE)
    .build();

wolk.connect();
```

This will establish the connection to platform and subscribe to channels
 used for actuation and configuration commands.
 
**Publishing sensor readings:**
```java
wolk.addReading("T", "24.5");

wolk.publish();
```

**Data publish strategy:**
If data persistence is disabled, sensor data will be sent immediatelly.
If data persistence is enabled, sensor data can be sent by calling
```java
wolk.publish(),
```
or enabling automatic publishing by calling
```java
wolk.startPublishing(intervalInSeconds).
```

**Disconnecting from the platform:**
```java
wolk.disconnect();
```
