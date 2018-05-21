package com.wolkabout.wolk.firmwareupdate;

public interface CommandReceivedProcessor {

    void onFileReady(byte[] bytes);

    void onInstallCommandReceived();

    void onAbortCommandReceived();
}
