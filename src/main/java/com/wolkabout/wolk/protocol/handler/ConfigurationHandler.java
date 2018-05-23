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
package com.wolkabout.wolk.protocol.handler;

import java.util.Map;

public interface ConfigurationHandler {

    /**
     * Called when configuration is received.
     *
     * @param configuration Key-value pair of references and values.
     */
    void onConfigurationReceived(Map<String, Object> configuration);

    /**
     * Called when configuration is requested by server.
     *
     * @return Key-value pairs of references and values.
     */
    Map<String, String> getConfigurations();
}
