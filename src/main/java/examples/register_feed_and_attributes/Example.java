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
                .host("ssl://integration5.wolkabout.com:8883")
                .sslCertification("ca.crt")
                .deviceKey("54507ee6-8437-487e-bbd9-4f7b948b528f")
                .password("6BBIJG1GA8")
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
