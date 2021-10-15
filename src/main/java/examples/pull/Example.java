package examples.pull;

import com.wolkabout.wolk.Wolk;
import com.wolkabout.wolk.model.Feed;
import com.wolkabout.wolk.model.OutboundDataMode;
import com.wolkabout.wolk.protocol.handler.FeedHandler;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

public class Example {
    public static void main(String[] args) {
        final Wolk wolk = Wolk.builder(OutboundDataMode.PULL)
                .mqtt()
                .host("ssl://integration5.wolkabout.com:8883")
                .sslCertification("ca.crt")
                .deviceKey("54507ee6-8437-487e-bbd9-4f7b948b528f")
                .password("6BBIJG1GA8")
                .build()
                .feed(new Handler())
                .build();

        while (true) {
            // connect will automatically pull feed values
            wolk.connect();

            double randomTemp = Math.random() * 100 - 20; // random number between -20 and 80

            wolk.addFeed("T", Double.toString(randomTemp));

            wolk.publish();

            try {
                // wait some time online to receive feed values
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            wolk.disconnect();

            // be offline for some time
            try {
                TimeUnit.SECONDS.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static class Handler extends FeedHandler {

        @Override
        public void onFeedsReceived(Collection<Feed> feeds) {

            for (Feed feed : feeds) {
                Object value = feed.getReference();
            }
        }

        @Override
        public Feed getFeedValue(String reference) {
            return null;
        }
    }
}
