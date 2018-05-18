package com.wolkabout.wolk.protocol.processor;

import java.util.Map;

public interface ConfigurationProcessor {

    /**
     * Called when configuration is received.
     *
     * @param configuration Key-value pair of references and values.
     */
    void onConfigurationReceived(Map<String, Object> configuration);
}
