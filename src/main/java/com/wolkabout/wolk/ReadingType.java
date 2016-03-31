package com.wolkabout.wolk;

/**
 * All supported reading types.
 */
public enum ReadingType {

    ACCELERATION("ACL"),
    GYRO("GYR"),
    MAGNET("MAG"),
    LIGHT("LT"),
    TEMPERATURE("T"),
    HUMIDITY("H"),
    PRESSURE("P"),
    BATTERY("B"),
    HEARTRATE("BPM"),
    STEPS("STP"),
    CALORIES("KCAL");

    private final String prefix;

    ReadingType(final String prefix) {
        this.prefix = prefix;
    }

    String getPrefix() {
        return prefix;
    }
}
