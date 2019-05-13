package examples.full_feature_set;

import com.wolkabout.wolk.Wolk;
import com.wolkabout.wolk.model.ActuatorCommand;
import com.wolkabout.wolk.model.ActuatorStatus;
import com.wolkabout.wolk.protocol.ProtocolType;
import com.wolkabout.wolk.protocol.handler.ActuatorHandler;
import com.wolkabout.wolk.protocol.handler.ConfigurationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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
//                .protocol(ProtocolType.JSON_SINGLE_REFERENCE)
                .protocol(ProtocolType.JSON)
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
                    public void onConfigurationReceived(Map<String, String> configuration) {
                        LOG.info("Configuration received " + configuration);

                        Values.configuration = configuration;
                    }

                    @Override
                    public Map<String, String> getConfigurations() {
                        return Values.configuration;
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

    static Map<String, String> configuration = new HashMap<String, String>();
    static {
        configuration.put("configuration_1", "0");
        configuration.put("configuration_2", "false");
        configuration.put("configuration_3", "Value");
        configuration.put("configuration_4", "Value1,Value2,Value3");
    };
}
