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
