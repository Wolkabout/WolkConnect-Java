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

## Example Usage


Create a device on WolkAbout IoT platform by using the provided *Simple example* device type.  
This template fits the [Simple example](https://github.com/Wolkabout/WolkConnect-Java-/blob/master/src/main/java/examples/simple/Example.java) and demonstrates the sending of a temperature sensor reading.
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
 
### Publishing sensor readings:
```java
wolk.addReading("T", "24.5");

// Multi-value sensor reading
wolk.addReading("ACL", Arrays.asList("0.4", "0.2", "0.0"));

wolk.publish();
```

### Publishing alarm events:
```java
wolk.addAlarm("HH", true);

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
Provide an implementation of `onActuationReceived` and `getActuatorStatus`:
```java
final Wolk wolk = Wolk.builder()
        .mqtt()
        .host("ssl://api-demo.wolkabout.com:8883")
        .sslCertification("ca.crt")
        .deviceKey("device_key")
        .password("some_password")
        .build()
        .protocol(ProtocolType.WOLKABOUT_PROTOCOL)
        .actuator(Arrays.asList("SW", "SL"), new ActuatorHandler() {
            @Override
            public void onActuationReceived(ActuatorCommand actuatorCommand) {
                LOG.info("Actuation received " + actuatorCommand.getReference() + " " +
                        actuatorCommand.getCommand() + " " + actuatorCommand.getValue());

                if (actuatorCommand.getCommand() == ActuatorCommand.CommandType.SET) {
                    if (actuatorCommand.getReference().equals("SL")) {
                        ActuatorValues.sliderValue = Double.parseDouble(actuatorCommand.getValue());
                    } else if (actuatorCommand.getReference().equals("SW")) {
                        ActuatorValues.switchValue = Boolean.parseBoolean(actuatorCommand.getValue());
                    }
                }
            }

            @Override
            public ActuatorStatus getActuatorStatus(String ref) {
                if (ref.equals("SL")) {
                    return new ActuatorStatus(ActuatorStatus.Status.READY, String.valueOf(ActuatorValues.sliderValue), "SL");
                } else if (ref.equals("SW")) {
                    return new ActuatorStatus(ActuatorStatus.Status.READY, String.valueOf(ActuatorValues.switchValue), "SW");
                }

                return new ActuatorStatus(ActuatorStatus.Status.ERROR, "", "");
            }
        })
```

Publish actuator status by calling:
```java
wolk.publishActuatorStatus("SW")
```
This will call `getActuatorStatus` and immediately try to publish to the Platform.

### Enabling device configuration:
Provide an implementation of `onConfigurationReceived` and `getConfigurations`:
```java
final Wolk wolk = Wolk.builder()
        .mqtt()
        .host("ssl://api-demo.wolkabout.com:8883")
        .sslCertification("ca.crt")
        .deviceKey("device_key")
        .password("some_password")
        .build()
        .protocol(ProtocolType.WOLKABOUT_PROTOCOL)
        .configuration(new ConfigurationHandler() {
            @Override
            public void onConfigurationReceived(Collection<Configuration> receivedConfigurations) {
                LOG.info("Configuration received " + receivedConfigurations);
                for (Configuration configuration : receivedConfigurations) {
                    if (configuration.getReference().equals("HB")) {
                        configurations.setHeartBeat(Integer.parseInt(configuration.getValue()));
                        continue;
                    }
                    if (configuration.getReference().equals("LL")) {
                        configurations.setLogLevel(configuration.getValue());
                        continue;
                    }
                    if (configuration.getReference().equals("EF")) {
                        configurations.setEnabledFeeds(new ArrayList<>(Arrays.asList(configuration.getValue().split(","))));
                    }
                }
                try {
                    objectMapper.writeValue(configurationFile, configurations);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Failed to save received configuration to file");
                }
            }

            @Override
            public Collection<Configuration> getConfigurations() {
                Collection<Configuration> currentConfigurations = new ArrayList<>();
                currentConfigurations.add(new Configuration("LL", configurations.getLogLevel()));
                currentConfigurations.add(new Configuration("EF", configurations.getEnabledFeeds()));
                currentConfigurations.add(new Configuration("HB", String.valueOf(configurations.getHeartBeat())));

                return currentConfigurations;
            }
        })
```


Publish configurations by calling:
```java
wolk.publishConfiguration()
```
This will call `getConfigurations` and immediately try to publish to the Platform.

### Ping keep-alive service

By default, the library publishes a keep alive message every 60 seconds to the Platform, to update the device's last report for cases when the device doesn't publish data often.
This service can be disabled to reduce bandwidth or battery usage:

```java
final Wolk wolk = Wolk.builder()
        .mqtt()
        .host("ssl://api-demo.wolkabout.com:8883")
        .sslCertification("ca.crt")
        .deviceKey("device_key")
        .password("some_password")
        .build()
        .enableKeepAliveService(false)
        .build();
```

Additionally, if this service is enabled and the device establishes connection to the Platform, then each keep alive message sent will be responded with the current UTC timestamp on the Platform.

This timestamp will be saved and updated for each response, and can be accessed with:

```java
long platformTimestamp = wolk.getPlatformTimestamp();
```
### File management & firmware update

*Note:* These features will be available in a later release.
