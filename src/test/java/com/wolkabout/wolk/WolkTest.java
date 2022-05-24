package com.wolkabout.wolk;

import org.junit.Test;

public class WolkTest {

    @Test
    public void connect() {
        Wolk wolk = Wolk.builder()
                .mqtt()
                .host("ssl://insert_host:insert_port")
                .sslCertification("/INSERT/PATH/TO/YOUR/CA.CRT/FILE")
                .deviceKey("device_key")
                .password("password")
                .build()
                .build();
    }

    @Test
    public void disconnect() {
    }

    @Test
    public void startPublishing() {
    }

    @Test
    public void stopPublishing() {
    }

    @Test
    public void publish() {
    }

    @Test
    public void addReading() {
    }

    @Test
    public void testAddReading() {
    }

    @Test
    public void testAddReading1() {
    }

    @Test
    public void addReadings() {
    }

    @Test
    public void addAlarm() {
    }

    @Test
    public void publishConfiguration() {
    }

    @Test
    public void publishActuatorStatus() {
    }

    @Test
    public void publishFirmwareVersion() {
    }

    @Test
    public void publishFileTransferStatus() {
    }

    @Test
    public void testPublishFileTransferStatus() {
    }

    @Test
    public void builder() {
    }
}