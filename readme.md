# WolkConnect

Connector library written in Java for WolkAbout platform.

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
    compile 'com.wolkabout:wolk:2.0.0'
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
  <version>2.0.0</version>
  <type>pom</type>
</dependency>
```
