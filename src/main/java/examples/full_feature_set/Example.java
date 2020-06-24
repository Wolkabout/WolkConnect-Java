/*
 * Copyright (c) 2019 WolkAbout Technology s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package examples.full_feature_set;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wolkabout.wolk.Wolk;
import com.wolkabout.wolk.model.ActuatorCommand;
import com.wolkabout.wolk.model.ActuatorStatus;
import com.wolkabout.wolk.model.Configuration;
import com.wolkabout.wolk.protocol.ProtocolType;
import com.wolkabout.wolk.protocol.handler.ActuatorHandler;
import com.wolkabout.wolk.protocol.handler.ConfigurationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Example {
    private static final Logger LOG = LoggerFactory.getLogger(Example.class);
    private final static ArrayList<String> VALID_LEVELS = new ArrayList<String>(Arrays.asList("TRACE", "DEBUG", "INFO", "WARN", "ERROR"));
    private final static String CONFIGURATION_FILE_PATH = "src/main/resources/configuration.json";

    public static void setLogLevel(String logLevel, String packageName) {
        if (!VALID_LEVELS.contains(logLevel)) {
            System.out.println(" Invalid level : " + logLevel);
            return;
        }
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger logger = loggerContext.getLogger(packageName);

        if (logger.getLevel() == Level.toLevel(logLevel)) {
            return;
        }

        System.out.println(packageName + " - current logger level: " + logger.getLevel());
        System.out.println("Setting logger level : " + logLevel);
        logger.setLevel(Level.toLevel(logLevel));
    }

    public static void main(String... args) throws IOException {

        ObjectMapper objectMapper = new ObjectMapper();
        File configurationFile = new File(CONFIGURATION_FILE_PATH);
        Configurations configurations = objectMapper.readValue(configurationFile, Configurations.class);
        setLogLevel(configurations.getLogLevel(), "com.wolkabout");

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
                .build();
        wolk.connect();
        wolk.publishActuatorStatus("SW");
        wolk.publishActuatorStatus("SL");
        wolk.publishConfiguration();


        if (configurations.getEnabledFeeds().contains("T")) {
            wolk.addReading("T", "25.6");
        }
        if (configurations.getEnabledFeeds().contains("P")) {
            wolk.addReading("P", "1024");
        }
        if (configurations.getEnabledFeeds().contains("H")) {
            wolk.addReading("H", "52");
            wolk.addAlarm("HH", true);
        }
        if (configurations.getEnabledFeeds().contains("ACL")) {
            wolk.addReading("ACL", Arrays.asList("1", "0", "0"));
        }


        wolk.publish();

        while (true) {
            try {
                TimeUnit.SECONDS.sleep(configurations.getHeartBeat());
            } catch (Exception e) {
            }
        }
    }
}

class ActuatorValues {
    static double sliderValue = 0;
    static boolean switchValue = false;

}

class Configurations {
    @JsonProperty("LL")
    private String logLevel;
    @JsonProperty("HB")
    private int heartBeat;
    @JsonProperty("EF")
    private ArrayList<String> enabledFeeds;

    @JsonProperty("LL")
    public String getLogLevel() {
        return logLevel;
    }

    @JsonProperty("LL")
    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    @JsonProperty("HB")
    public int getHeartBeat() {
        return heartBeat;
    }

    @JsonProperty("HB")
    public void setHeartBeat(int heartBeat) {
        this.heartBeat = heartBeat;
    }

    @JsonProperty("EF")
    public ArrayList<String> getEnabledFeeds() {
        return enabledFeeds;
    }

    @JsonProperty("EF")
    public void setEnabledFeeds(ArrayList<String> enabledFeeds) {
        this.enabledFeeds = enabledFeeds;
    }
}
