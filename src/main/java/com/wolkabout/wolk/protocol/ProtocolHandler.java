package com.wolkabout.wolk.protocol;

import com.wolkabout.wolk.model.ActuatorCommand;
import com.wolkabout.wolk.model.ActuatorStatus;

import java.util.Map;

public interface ProtocolHandler {

    /**
     * When the actuation command is given from the platform, it will be delivered to this method.
     * This method should pass the new value for the actuator to device.
     *
     * @param actuatorCommand {@link ActuatorCommand}
     */
    void onActuationReceived(ActuatorCommand actuatorCommand);

    /**
     * Called when configuration is received.
     *
     * @param configuration Key-value pair of references and values.
     */
    void onConfigurationReceived(Map<String, Object> configuration);

    /**
     * Reads the status of actuator from device and returns it as ActuatorStatus object.
     *
     * @param ref of the actuator.
     * @return ActuatorStatus object.
     */
    ActuatorStatus getActuatorStatus(String ref);

    /**
     * Called when configuration is requested by server.
     *
     * @return Key-value pairs of references and values.
     */
    Map<String, String> getConfigurations();

}
