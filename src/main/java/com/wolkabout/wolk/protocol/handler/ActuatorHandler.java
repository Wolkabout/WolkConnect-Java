package com.wolkabout.wolk.protocol.handler;

import com.wolkabout.wolk.model.ActuatorCommand;
import com.wolkabout.wolk.model.ActuatorStatus;

public interface ActuatorHandler {

    /**
     * When the actuation command is given from the platform, it will be delivered to this method.
     * This method should pass the new value for the actuator to device.
     *
     * @param actuatorCommand {@link ActuatorCommand}
     */
    void onActuationReceived(ActuatorCommand actuatorCommand);

    /**
     * Reads the status of actuator from device and returns it as ActuatorStatus object.
     *
     * @param ref of the actuator.
     * @return ActuatorStatus object.
     */
    ActuatorStatus getActuatorStatus(String ref);
}
