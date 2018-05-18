package com.wolkabout.wolk.protocol.provider;

import java.util.Map;

public interface ConfigurationProvider {

    /**
     * Called when configuration is requested by server.
     *
     * @return Key-value pairs of references and values.
     */
    Map<String, String> getConfigurations();
}
