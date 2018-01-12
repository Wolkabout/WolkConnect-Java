/*
 * Copyright (c) 2017 WolkAbout Technology s.r.o.
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
package com.wolkabout.wolk;

/**
 * Device for which the connection is established.
 */
public class Device {

    /**
     * Serial number obtained for the device registration.
     */
    private final String deviceKey;

    /**
     * Password obtained after the device has been registered.
     */
    private String password;

    private String[] actuators;

    private String[] config;

    private Protocol protocol = Protocol.JSON_SINGLE;

    public Device(String deviceKey) {
        this.deviceKey = deviceKey;
        this.actuators = new String[]{};
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    public String[] getConfig() {
        return config;
    }

    public void setConfig(String[] config) {
        this.config = config;
    }

    public String[] getActuators() {
        return actuators;
    }

    /**
     * Provide list of references to actuators on the destination machine.
     *
     * @param actuators references to actuators on device.
     */
    public void setActuators(String... actuators) {
        this.actuators = actuators;
    }

    public String getDeviceKey() {
        return deviceKey;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

}
