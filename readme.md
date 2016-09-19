# WolkConnect

Connector library written in Java for WolkAbout Smart IoT Cloud.
This version publishes data via MQTT protocol to [WolkSense.com](https://wolksense.com/) cloud instance.

You can import it in your project using gradle or maven configurations below.

Gradle
------

```sh
repositories {
    maven {
        url  "http://dl.bintray.com/wolkabout/WolkConnector"
    }
}

dependencies {
    compile 'com.wolkabout:wolk:1.0.1'
}
```
Maven
-----
```sh
<repository>
    <id>bintray-wolkabout-WolkConnector</id>
    <name>bintray</name>
    <url>http://dl.bintray.com/wolkabout/WolkConnector</url>
</repository>

<dependency>
  <groupId>com.wolkabout</groupId>
  <artifactId>wolk</artifactId>
  <version>1.0.1</version>
  <type>pom</type>
</dependency>
```