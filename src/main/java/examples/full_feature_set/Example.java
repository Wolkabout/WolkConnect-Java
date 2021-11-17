/*
 * Copyright (c) 2021 WolkAbout Technology s.r.o.
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
package examples.full_feature_set;

import com.wolkabout.wolk.Wolk;
import com.wolkabout.wolk.firmwareupdate.FirmwareInstaller;
import com.wolkabout.wolk.model.*;
import com.wolkabout.wolk.protocol.handler.FeedHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

public class Example {
    private static final Logger LOG = LoggerFactory.getLogger(Example.class);

    private static final String NEW_FEED_REF = "NF";
    private static final String version = "1.0";

    public static void main(String... args) throws IOException {

        final InOutFeeds feeds = new InOutFeeds();

        final Wolk wolk = Wolk.builder(OutboundDataMode.PUSH)
                .mqtt()
                .host(Wolk.WOLK_DEMO_URL)
                .sslCertification(Wolk.WOLK_DEMO_CA)
                .deviceKey("device-key")
                .password("device-password")
                .build()
                .feed(new FeedHandler() {
                    @Override
                    public void onFeedsReceived(Collection<Feed> receivedFeeds) {
                        LOG.info("Feeds received " + receivedFeeds);
                        for (Feed feed : receivedFeeds) {
                            if (feed.getReference().equals(feeds.getHeartbeatReference())) {
                                feeds.setHeartbeatValue(feed.getNumericValue().longValue());
                            } else if (feed.getReference().equals(feeds.getSwitchReference())) {
                                feeds.setSwitchValue(feed.getBooleanValue());
                            }
                        }
                    }

                    @Override
                    public Feed getFeedValue(String reference) {
                        if (reference.equals(feeds.getHeartbeatReference())) {
                            return new Feed(feeds.heartbeatReference, String.valueOf(feeds.heartbeatValue));
                        } else if (reference.equals(feeds.getSwitchReference())) {
                            return new Feed(feeds.getSwitchReference(), String.valueOf(feeds.switchValue));
                        }

                        return null;
                    }
                })
                .enableFileManagement("files/")
                .enableFirmwareUpdate(new FirmwareInstaller() {

                    private boolean aborted = false;

                    @Override
                    public boolean onInstallCommandReceived(String fileName) {
                        try {
                            Thread.sleep(10000);
                            if (!aborted) {
                                System.exit(0);
                                return true;
                            }
                        } catch (InterruptedException ignored) {
                        }
                        return false;
                    }

                    @Override
                    public void onAbortCommandReceived() {
                        aborted = true;
                    }

                    @Override
                    public String getFirmwareVersion() {
                        return version;
                    }
                })
                .build();

        wolk.connect();

        wolk.registerFeed("New Feed", FeedType.IN, Unit.NUMERIC, NEW_FEED_REF);

        wolk.registerAttribute("Device activation timestamp", DataType.NUMERIC, String.valueOf(System.currentTimeMillis()));

        while (true) {

            try {
                double temperature = Math.random() * 100 - 20; // random number between -20 and 80
                wolk.addFeed("T", temperature);

                double randomValue = Math.random() * 100;
                wolk.addFeed(NEW_FEED_REF, randomValue);

                wolk.publish();

                TimeUnit.SECONDS.sleep(feeds.getHeartbeatValue());
            } catch (Exception e) {
                System.out.println(e.getLocalizedMessage());
            }
        }
    }
}

class InOutFeeds {
    final String switchReference = new String("SW");
    boolean switchValue = false;

    final String heartbeatReference = new String("HB");
    long heartbeatValue = 120;

    public String getSwitchReference() {
        return switchReference;
    }

    public String getHeartbeatReference() {
        return heartbeatReference;
    }

    public boolean getSwitchValue() {
        return switchValue;
    }

    public void setSwitchValue(boolean switchValue) {
        this.switchValue = switchValue;
    }

    public long getHeartbeatValue() {
        return heartbeatValue;
    }

    public void setHeartbeatValue(long heartbeatValue) {
        this.heartbeatValue = heartbeatValue;
    }
}
