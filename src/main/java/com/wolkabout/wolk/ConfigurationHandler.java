/*
 * Copyright (c) 2018 WolkAbout Technology s.r.o.
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

import java.util.Map;

/*
 * Provide implementation of ConfigurationHandler to pass configuration parameters from platform to your device.
 */
public interface ConfigurationHandler {
    /**
     * When new set of device configuration values is given from platform, it will be delivered to this method.
     * This method should update device configuration with received configuration values.
     *
     * @param configuration Map<String, String> with device configuration reference as map key,
     *                      and device configuration value as map value
     */
    void handleConfiguration(Map<String, String> configuration);
}
