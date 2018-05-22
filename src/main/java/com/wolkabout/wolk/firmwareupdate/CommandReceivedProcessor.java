package com.wolkabout.wolk.firmwareupdate;

public interface CommandReceivedProcessor {

    void onFileReady(String fileName, boolean autoInstall, byte[] bytes);

    void onInstallCommandReceived();

    void onAbortCommandReceived();
}
