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

WolkAbout Java Connector for connecting devices to  [WolkAbout IoT Platform](https://demo.wolkabout.com/#/login).

Supported device communication protocols:
* WolkAbout Protocol


Prerequisite
------

You can import it in your project using gradle or maven configurations below.  
Include WolkConnect-Java library to project.

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

Create a device on WolkAbout IoT platform by using the provided *Simple example* device type.  
This template fits the [Simple example](https://github.com/Wolkabout/WolkConnect-Java-/blob/master/src/main/java/examples/simple/Example.java) and demonstrates the sending of a temperature sensor reading.

**Establishing MQTT connection with the platform:**
```java
final Wolk wolk = Wolk.builder()
    .mqtt()
        .host("ssl://api-demo.wolkabout.com:8883")
        .sslCertification("ca.crt")
        .deviceKey("devicekey")
        .password("password")
        .build()
    .protocol(ProtocolType.WOLKABOUT_PROTOCOL)
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
If data persistence is disabled, sensor data will be sent immediately.
If data persistence is enabled, sensor data can be sent by calling:
```java
wolk.publish();
```
or enabling automatic publishing by calling
```java
wolk.startPublishing(intervalInSeconds);
```

**Disconnecting from the platform:**
```java
wolk.disconnect();
```

**Additional functionality:**

WolkConnect-Java library has integrated additional features which can perform full WolkAbout IoT platform potential. Read more about full feature set example [HERE](https://github.com/Wolkabout/WolkConnect-Java-/blob/master/src/main/java/examples/full_feature_set/Example.java).