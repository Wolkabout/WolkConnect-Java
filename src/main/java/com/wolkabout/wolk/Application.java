package com.wolkabout.wolk;

import com.wolkabout.wolk.model.ActuatorCommand;
import com.wolkabout.wolk.model.ActuatorStatus;
import com.wolkabout.wolk.protocol.ProtocolType;
import com.wolkabout.wolk.protocol.handler.ActuatorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    public static void main(String... args) {
        Wolk.builder()
                .mqtt()
                    .host("tcp://localhost")
                    .deviceKey("54auv4do2i9pzofp")
                    .password("a0b8890e-63b0-430d-8a76-8a9fef9d84f8")
                    .build()
                .protocol(ProtocolType.JSON_SINGLE_REFERENCE)
                .actuator(new ActuatorHandler() {
                    @Override
                    public void onActuationReceived(ActuatorCommand actuatorCommand) {
                        LOG.info("Received actiations: " + actuatorCommand);
                        getWolk().publishActuatorStatus(actuatorCommand.getReference());
                    }

                    @Override
                    public ActuatorStatus getActuatorStatus(String ref) {
                        return new ActuatorStatus(ActuatorStatus.Status.READY, "false", ref);
                    }
                })
                .connect();
    }

}
