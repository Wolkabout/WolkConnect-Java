# WolkConnect

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
    compile 'com.wolkabout:wolk:2.6.6'
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
  <version>2.6.6</version>
  <type>pom</type>
</dependency>
```

Example Usage
-------------
**Establishing mqtt connection with the platform:**
```java
import com.wolkabout.wolk.protocol.ProtocolType;

public class Example {

    public static void main(String... args) {
        final Wolk wolk = Wolk.builder()
                .mqtt()
                    .host("tcp://localhost")
                    .deviceKey("devicekey")
                    .password("password")
                    .build()
                .protocol(ProtocolType.JSON)
                .connect();

        wolk.addReading("T", "24.5");
        wolk.addReading("P", "956");
        wolk.addReading("H", "67.3");
        wolk.publish();
    }
}
```

This will establish the connection to platform and subscribe to channels
 used for actuation and configuration commands.
 

**Publishing data:**
If data persistence is disabled, sensor data will be sent immediatelly.
If data persistence is enabled, sensor data can be sent by calling wolk.publish(),
or enabling automatic publishing by calling wolk.startPublishing(intervalInSeconds).

**Disconnecting from the platform:**
```java
wolk.disconnect();
```