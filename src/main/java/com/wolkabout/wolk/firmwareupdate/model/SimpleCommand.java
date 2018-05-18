/*
 * Copyright (c) 2018 Wolkabout
 */

package com.wolkabout.wolk.firmwareupdate.model;


public class SimpleCommand {

    private String command;

    public SimpleCommand() {}

    public SimpleCommand(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    @Override
    public String toString() {
        return "SimpleCommand{" +
                "command='" + command + '\'' +
                '}';
    }
}
