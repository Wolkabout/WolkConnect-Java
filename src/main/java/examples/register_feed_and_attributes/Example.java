package examples.register_feed_and_attributes;

import com.wolkabout.wolk.Wolk;
import com.wolkabout.wolk.model.DataType;
import com.wolkabout.wolk.model.FeedType;
import com.wolkabout.wolk.model.OutboundDataMode;
import com.wolkabout.wolk.model.Unit;

import java.util.concurrent.TimeUnit;

public class Example {
    public static void main(String[] args) {

        final String FEED_REF = new String("NF");

        final Wolk wolk = Wolk.builder(OutboundDataMode.PUSH)
                .mqtt()
                .host(Wolk.WOLK_DEMO_URL)
                .sslCertification(Wolk.WOLK_DEMO_CA)
                .deviceKey("device-key")
                .password("device-password")
                .build()
                .build();

        wolk.connect();

        wolk.registerFeed("New Feed", FeedType.IN, Unit.NUMERIC, FEED_REF);

        wolk.registerAttribute("Device activation timestamp", DataType.NUMERIC, String.valueOf(System.currentTimeMillis()));

        while (true) {
            try {
                double randomValue = Math.random() * 100 - 20; // random number between -20 and 80

                wolk.addFeed(FEED_REF, randomValue);

                wolk.publish();

                TimeUnit.SECONDS.sleep(5);
            } catch (Exception e) {
            }
        }
    }
}
