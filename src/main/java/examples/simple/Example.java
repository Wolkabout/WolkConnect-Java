/*
 * Copyright (c) 2019 WolkAbout Technology s.r.o.
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
package examples.simple;

import com.wolkabout.wolk.Wolk;
import com.wolkabout.wolk.model.OutboundDataMode;

import java.util.concurrent.TimeUnit;

public class Example {
    public static void main(String[] args) {

        final Wolk wolk = Wolk.builder(OutboundDataMode.PUSH)
                .mqtt()
                .host(Wolk.WOLK_DEMO_URL)
                .sslCertification(Wolk.WOLK_DEMO_CA)
                .deviceKey("device-key")
                .password("device-password")
                .build()
                .build();

        wolk.connect();

        while (true) {
            try {
                double randomTemp = Math.random() * 100 - 20; // random number between -20 and 80

                wolk.addFeed("T", Double.toString(randomTemp));

                wolk.publish();

                TimeUnit.SECONDS.sleep(5);
            } catch (Exception e) {
            }
        }
    }
}
