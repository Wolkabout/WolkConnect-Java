package com.wolkabout.wolk;

import com.wolkabout.wolk.firmwareupdate.CommandReceivedProcessor;
import com.wolkabout.wolk.model.ActuatorCommand;
import com.wolkabout.wolk.model.ActuatorStatus;
import com.wolkabout.wolk.protocol.ProtocolHandler;
import com.wolkabout.wolk.protocol.ProtocolType;

import java.util.Map;

public class Example {
    
    public void test() {
        final Wolk wolk = new Wolk.Builder()
                .mqtt()
                    .host("Some host")
                    .deviceKey("key1234")
                    .password("password")
                    .build()
                .protocolType(ProtocolType.JSON_SINGLE_REFERENCE)
                .protocolHandler(new ProtocolHandler() {
                    @Override
                    public void onActuationReceived(ActuatorCommand actuatorCommand) {

                    }

                    @Override
                    public void onConfigurationReceived(Map<String, Object> configuration) {

                    }

                    @Override
                    public ActuatorStatus getActuatorStatus(String ref) {
                        return null;
                    }

                    @Override
                    public Map<String, String> getConfigurations() {
                        return null;
                    }
                })
                .enableFirmwareUpdate(new CommandReceivedProcessor() {
                    @Override
                    public void onFileReady(byte[] bytes) {
                        
                    }

                    @Override
                    public void onInstallCommandReceived() {

                    }

                    @Override
                    public void onAbortCommandReceived() {

                    }
                }).connect();
    }
}
