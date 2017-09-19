package com.wolkabout.wolk;

/**
 * Copyright Wolkabout 2017
 */
public class ActuatorCommand {

    public enum Command {SET, STATUS, UNKOWN}

    private String command;
    private String value;

    public Command getCommand() {
        try {
            return Command.valueOf(this.command);
        } catch (IllegalArgumentException e) {
            return Command.UNKOWN;
        }
    }

    public String getValue() {
        return value;
    }
}
