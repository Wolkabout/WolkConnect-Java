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

[![Build Status](https://travis-ci.com/Wolkabout/WolkConnect-Java.svg?branch=master)](https://travis-ci.com/Wolkabout/WolkConnect-Java)

WolkAbout Java Connector for connecting devices to  [WolkAbout IoT Platform](https://demo.wolkabout.com/#/login).

Supported device communication protocols:
* WolkAbout Protocol


## Prerequisite


You can import it in your project using gradle or maven configurations below.  
Include WolkConnect-Java library to project.

Gradle

```groovy
repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    compile 'com.wolkabout:wolk:4.0.0'
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
  <version>4.0.0</version>
  <type>pom</type>
</dependency>
```

## Example Usage


Create a device on WolkAbout IoT platform by using the provided *Simple example* device type.  
This device type fits the [Simple example](https://github.com/Wolkabout/WolkConnect-Java-/blob/master/src/main/java/examples/simple/Example.java) and demonstrates periodic the sending of a temperature sensor reading.
After creating the device on the Platform, copy the provided credentials into `deviceKey` and `password`.

### Establishing MQTT connection with the platform:
```java
final Wolk wolk = Wolk.builder()
    .mqtt()
        .host("ssl://api-demo.wolkabout.com:8883")
        .sslCertification("ca.crt")
        .deviceKey("devicekey")
        .password("password")
        .build()
    .build();

wolk.connect();
```

This will establish the connection to platform and subscribe to channels
 used for actuation and configuration commands.
 
### Adding feed values:
```java
wolk.addFeed("T", 24.5);

// Multi-value sensor reading
wolk.addFeed("ACL", Arrays.asList(0.4, 0.2, 0.0));

wolk.publish();
```

### Data publish strategy:
If data persistence is disabled, sensor data and alarms will be sent immediately.
If data persistence is enabled, sensor data and alarms can be sent by calling:
```java
wolk.publish();
```
or enabling automatic publishing by calling
```java
wolk.startPublishing(intervalInSeconds);
```

### Disconnecting from the platform:
```java
wolk.disconnect();
```

## Additional functionality:

WolkConnect-Java library has integrated additional features which can perform full WolkAbout IoT platform potential. See the full feature set example [HERE](https://github.com/Wolkabout/WolkConnect-Java-/blob/master/src/main/java/examples/full_feature_set/Example.java).

### Enabling device actuators:
Provide an implementation of `onFeedsReceived` and `getActuatorStatus`:
```java
final Wolk wolk = Wolk.builder()
        .mqtt()
        .host("ssl://api-demo.wolkabout.com:8883")
        .sslCertification("ca.crt")
        .deviceKey("device_key")
        .password("some_password")
        .build()
        .protocol(ProtocolType.WOLKABOUT_PROTOCOL)
		.feed(new FeedHandler() {
            @Override
            public void onFeedsReceived(Collection<Feed> receivedFeeds) {
                LOG.info("Feeds received " + receivedFeeds);
                for (Feed feed : receivedFeeds) {
                    if (feed.getReference().equals(feeds.getHeartbeatReference())) {
                        feeds.setHeartbeatValue(feed.getNumericValue().longValue());
                    } else if (feed.getReference().equals(feeds.getSwitchReference())) {
                        feeds.setSwitchValue(feed.getBooleanValue());
                    }
                }
            }

            @Override
            public Feed getFeedValue(String reference) {
                if (reference.equals(feeds.getHeartbeatReference())) {
                    return new Feed(feeds.heartbeatReference, String.valueOf(feeds.heartbeatValue));
                } else if (reference.equals(feeds.getSwitchReference())) {
                    return new Feed(feeds.getSwitchReference(), String.valueOf(feeds.switchValue));
                }

                return null;
            }
        })
```

### File management & firmware update

To enable these features, you need to invoke the methods in the builder.

##### File Management

```java
final Wolk wolk = Wolk.builder()
    .mqtt()
    .host("ssl://api-demo.wolkabout.com:8883")
    .sslCertification("ca.crt")
    .deviceKey("device_key")
    .password("some_password")
    .enableFileManagement()
    .build();

/** You can use
    enableFileManagement() - use everything default
    enableFileManagement(String fileManagementLocation) - use a specific file location
    enableFileManagement(UrlFileDownloader urlFileDownloader) - use a custom url file downloader
    enableFileManagement(String fileManagementLocation, UrlFileDownloader urlFileDownloader) - use both custom **/
```

You might want to implement a custom `UrlFileDownloader` object. This allows you to inject custom logic for downloading
the file, based on the given URL. The default HTTP location will just target the request with GET method, without any
arguments/headers.

The [interface](https://github.com/Wolkabout/WolkConnect-Java/blob/master/src/main/java/com/wolkabout/wolk/filemanagement/UrlFileDownloader.java)
looks like this:
```java
public interface UrlFileDownloader {
    Map.Entry<FileTransferStatus, FileTransferError> downloadFile(String fileUrl);
}
```

##### Firmware Update
*Very important note* - if you have not enabled the file management, this will enable it. You can pass arguments through
this call to, to configure the file management in the way you want.

```java
final FirmwareInstaller firmwareInstaller = ...;
String firmwareVersion = ...;

final Wolk wolk = Wolk.builder()
    .mqtt()
    .host("ssl://api-demo.wolkabout.com:8883")
    .sslCertification("ca.crt")
    .deviceKey("device_key")
    .password("some_password")
    .enableFirmwareUpdate(firmwareInstaller, firmwareVersion)
    .build();

/** You can use
    enableFirmwareUpdate(FirmwareInstaller firmwareInstaller, String firmwareVersion) - use default file management, and pass the firmware installer and firmware version
    enableFirmwareUpdate(String fileManagementLocation, String firmwareVersion, FirmwareInstaller firmwareInstaller) - configure the file management with the location
    enableFirmwareUpdate(UrlFileDownloader urlFileDownloader, String firmwareVersion, FirmwareInstaller firmwareInstaller) - configure the file management with the url file downloader
    enableFirmwareUpdate(String fileManagementLocation, UrlFileDownloader urlFileDownloader, String firmwareVersion, FirmwareInstaller firmwareInstaller) - configure the file management with everything custom **/
```

You do need to implement a object from `FirmwareInstaller` interface. This allows you to implement some logic for your
use case to handle the incoming firmware update initialization messages, abort messages and to provide a firmware version.

The [interface](https://github.com/Wolkabout/WolkConnect-Java/blob/master/src/main/java/com/wolkabout/wolk/firmwareupdate/FirmwareInstaller.java)
looks like this:
```java
public interface FirmwareInstaller {
    boolean onInstallCommandReceived(String fileName);
    void onAbortCommandReceived();
    String onFirmwareVersion();
}
```
