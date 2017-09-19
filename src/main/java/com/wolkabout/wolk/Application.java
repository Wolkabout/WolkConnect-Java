package com.wolkabout.wolk;

import java.util.Scanner;

/**
 * Copyright Wolkabout 2017
 */
public class Application {

    public static void main(String[] args) {


        Device device = new Device();
        device.setSerialId("SN123");
        device.setPassword("");
        device.setActuators(new String[]{"AS1", "AS2"});

        Wolk wolk = new Wolk(device, "localhost");
        try {
            wolk.setActuatorStatusProvider(ref -> new ActuatorStatus("OK", "ON"));
            wolk.connect((reference, value) -> {
                System.out.println("Ref: " + reference + " value: " + value);
            });
            System.out.println("Enter your message: ");
            Scanner scanner = new Scanner(System.in);
            String reading;
            while (!(reading = scanner.nextLine()).equals("end")) {
                if (reading.equals("pub")) {
                    wolk.publish();
                } else {
                    if (reading.contains("/")) {
                        final String[] topicReading = reading.split("/");
                        wolk.addReading(topicReading[0], topicReading[1]);
                    } else {
                        final String[] topicReading = reading.split(":");
                        wolk.addReading(ReadingType.fromPrefix(topicReading[0]), topicReading[1]);
                    }
                }
            }
            wolk.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
