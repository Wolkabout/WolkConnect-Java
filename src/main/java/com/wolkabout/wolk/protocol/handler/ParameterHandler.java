package com.wolkabout.wolk.protocol.handler;

import com.wolkabout.wolk.model.Parameter;

import java.util.Collection;

public abstract class ParameterHandler {

    /**
     * Called when parameters are received.
     *
     * @param parameters Collection of key-value pair of names and values.
     */
    public abstract void onParametersReceived(Collection<Parameter> parameters);
}
