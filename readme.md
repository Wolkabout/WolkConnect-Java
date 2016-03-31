# WolkConnect

First version of the connector library written in Java for WolkAbout Smart IoT Cloud.
This version publishes data via MQTT protocol to [WolkSense.com](https://wolksense.com/) WolkSense.com cloud instance.

Gradle
------

```sh
repositories {
    maven {
        url  "http://dl.bintray.com/wolkabout/WolkConnector"
    }
}

dependencies {
    compile 'com.wolkabout:wolk:1.0'
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
  <version>1.0</version>
  <type>pom</type>
</dependency>
```