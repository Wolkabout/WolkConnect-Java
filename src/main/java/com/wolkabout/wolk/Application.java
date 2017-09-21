/*
 * Copyright (c) 2017 WolkAbout Technology s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.wolkabout.wolk;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Application {

    public static void main(String[] args) {

        Device device = new Device();
//        device.setSerialId("1017JAVA00002");
//        device.setPassword("bd0a4746-9030-4cb0-be99-a7d7735880ef");
        device.setSerialId("1017JAVA00003");
        device.setPassword("3b30733b-5d9c-4bc3-bf84-c28fafdf451e");
        device.setActuators(new String[]{"AS1", "AS2"});

        final Map<String, String> actuators = new HashMap<>();

        try {
            final Wolk wolk = Wolk.connectDevice(device)
                    .actuationHandler((reference, value) -> {
                        actuators.put(reference, value);
                        System.out.println("Ref: " + reference + " value: " + value);
                    })
                    .actuatorStatusProvider(ref -> new ActuatorStatus("OK", actuators.get(ref)))
                    .connect();

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
                    } else if (reading.contains(":")) {
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
