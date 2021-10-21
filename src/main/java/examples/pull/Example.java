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
                .host(Wolk.WOLK_DEMO_URL)
                .sslCertification(Wolk.WOLK_DEMO_CA)
                .deviceKey("device-key")
                .password("device-password")
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
