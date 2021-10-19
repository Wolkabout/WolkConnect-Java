package com.wolkabout.wolk.protocol.handler;

public abstract class TimeHandler {

    /**
     * Called when time is received.
     *
     * @param timestamp in milliseconds.
     */
    public abstract void onTimeReceived(long timestamp);
}
