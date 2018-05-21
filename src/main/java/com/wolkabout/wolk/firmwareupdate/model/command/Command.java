/*
 * Copyright (c) 2018 Wolkabout
 */

package com.wolkabout.wolk.firmwareupdate.model.command;


public class Command {

    private String command;

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    @Override
    public String toString() {
        return "Command{" +
                "command='" + command + '\'' +
                '}';
    }
}
