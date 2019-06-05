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

import com.wolkabout.wolk.Wolk;
import com.wolkabout.wolk.firmwareupdate.FirmwareInstaller;
import com.wolkabout.wolk.firmwareupdate.model.FirmwareStatus;
import com.wolkabout.wolk.model.ActuatorCommand;
import com.wolkabout.wolk.model.ActuatorStatus;
import com.wolkabout.wolk.model.Configuration;
import com.wolkabout.wolk.protocol.ProtocolType;
import com.wolkabout.wolk.protocol.handler.ActuatorHandler;
import com.wolkabout.wolk.protocol.handler.ConfigurationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class Example {
    private static final Logger LOG = LoggerFactory.getLogger(Example.class);

    public static void main(String... args) {

        final Wolk wolk = Wolk.builder()
                .mqtt()
                .host("ssl://api-demo.wolkabout.com:8883")
                .sslCertification("ca.crt")
                .deviceKey("device_key")
                .password("some_password")
                .build()
                .protocol(ProtocolType.JSON_SINGLE_REFERENCE)
                .actuator(Arrays.asList("SL", "SW"), new ActuatorHandler() {
                    @Override
                    public void onActuationReceived(ActuatorCommand actuatorCommand) {
                        LOG.info("Actuation received " + actuatorCommand.getReference() + " " +
                                actuatorCommand.getCommand() + " " + actuatorCommand.getValue());

                        if (actuatorCommand.getCommand() == ActuatorCommand.CommandType.SET) {
                            if (actuatorCommand.getReference().equals("SL")) {
                                Values.sliderValue = Double.parseDouble(actuatorCommand.getValue());
                            } else if (actuatorCommand.getReference().equals("SW")) {
                                Values.switchValue = Boolean.parseBoolean(actuatorCommand.getValue());
                            }
                        }
                    }

                    @Override
                    public ActuatorStatus getActuatorStatus(String ref) {
                        if (ref.equals("SL")) {
                            return new ActuatorStatus(ActuatorStatus.Status.READY, String.valueOf(Values.sliderValue), "SL");
                        } else if (ref.equals("SW")) {
                            return new ActuatorStatus(ActuatorStatus.Status.READY, String.valueOf(Values.switchValue), "SW");
                        }

                        return new ActuatorStatus(ActuatorStatus.Status.ERROR, "", "");
                    }
                })
                .configuration(new ConfigurationHandler() {
                    @Override
                    public void onConfigurationReceived(Collection<Configuration> configurations) {
                        LOG.info("Configuration received " + configurations);

                        Values.configurations = configurations;
                    }

                    @Override
                    public Collection<Configuration> getConfigurations() {
                        return Values.configurations;
                    }
                })
                .enableFirmwareUpdate(new FirmwareInstaller() {
                    @Override
                    public void onFileReady(String fileName, boolean autoInstall, byte[] bytes) {
                        LOG.info("Firmware file ready, autoinstall: " + autoInstall);

                        if (autoInstall) {
                            getWolk().publishFirmwareUpdateStatus(FirmwareStatus.INSTALLATION);

                            try {
                                TimeUnit.MILLISECONDS.sleep(2000);
                            } catch (Exception e) {}

                            getWolk().publishFirmwareUpdateStatus(FirmwareStatus.COMPLETED);

                            getWolk().publishFirmwareVersion(Integer.toString(++Values.firmwareVersion) + ".0.0");
                        }
                    }

                    @Override
                    public void onInstallCommandReceived() {
                        LOG.info("Firmware install");
                        getWolk().publishFirmwareUpdateStatus(FirmwareStatus.INSTALLATION);

                        try {
                            TimeUnit.MILLISECONDS.sleep(2000);
                        } catch (Exception e) {}

                        getWolk().publishFirmwareUpdateStatus(FirmwareStatus.COMPLETED);

                        getWolk().publishFirmwareVersion(Integer.toString(++Values.firmwareVersion) + ".0.0");
                    }

                    @Override
                    public void onAbortCommandReceived() {
                        LOG.info("Firmware installation abort");
                        getWolk().publishFirmwareUpdateStatus(FirmwareStatus.ABORTED);
                    }
                })
                .build();

        wolk.connect();

        wolk.addAlarm("HH", true);

        wolk.addReading("P", "1024");
        wolk.addReading("T", "25.6");
        wolk.addReading("H", "52");

        wolk.addReading("ACL", Arrays.asList("1", "0", "0"));

        wolk.publish();

        while (true) {
            try {
                TimeUnit.MILLISECONDS.sleep(200);
            } catch (Exception e) {}
        }
    }
}

class Values {
    static double sliderValue = 0;
    static boolean switchValue = false;

    static int firmwareVersion = 1;

    static Collection<Configuration> configurations = new ArrayList<>();

    static {
        configurations.add(new Configuration("config_1", "0"));
        configurations.add(new Configuration("config_2", "false"));
        configurations.add(new Configuration("config_3", "Value, test"));
        configurations.add(new Configuration("config_4", Arrays.asList("Value1", "Value2", "Value3")));
    };
}
