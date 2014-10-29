import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Hosts;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.event.Event;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by wzwang on 14/10/20.
 */
public class TwitterStream {
    static Properties twitterKey;
    static TweetServer tweetServer;
    static int port = 11083;

    public static void main(String[] args) {
        twitterKey = new Properties();
        try {
            tweetServer = new TweetServer(port);
            tweetServer.start();
            System.out.println( "TweetServer started on port: " + tweetServer.getPort());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        try {
            twitterKey.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("TwitterKey.ini"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        BlockingQueue<String> msgQueue = new LinkedBlockingQueue<String>(100000);
        BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<Event>(1000);
        Thread t = new QueueThread(msgQueue);
        t.start();


/** Declare the host you want to connect to, the endpoint, and authentication (basic auth or oauth) */
        Hosts hosebirdHosts = new HttpHosts(Constants.STREAM_HOST);
        StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();
        List<String> terms = Lists.newArrayList("food", "movie", "party", "soccer");
        //hosebirdEndpoint.followings(followings);
        hosebirdEndpoint.trackTerms(terms);


// These secrets should be read from a config file
        Authentication hosebirdAuth = new OAuth1(twitterKey.getProperty("consumerKey"), twitterKey.getProperty("consumerSecret"), twitterKey.getProperty("token"), twitterKey.getProperty("tokenSecret"));
        ClientBuilder builder = new ClientBuilder()
                .name("Hosebird-Client-01")                              // optional: mainly for the logs
                .hosts(hosebirdHosts)
                .authentication(hosebirdAuth)
                .endpoint(hosebirdEndpoint)
                .processor(new StringDelimitedProcessor(msgQueue))
                .eventMessageQueue(eventQueue);                          // optional: use this if you want to process client events

        Client hosebirdClient = builder.build();
// Attempts to establish a connection.
        hosebirdClient.connect();
    }

    public static class QueueThread extends Thread {
        BlockingQueue<String> msgQueue;

        public QueueThread(BlockingQueue<String> msgQueue) {
            super();
            this.msgQueue = msgQueue;
        }

        public void run() {
            Gson gson = new Gson();
            while (true) {
                try {
                    String msg = msgQueue.take();
                    Tweet tweet = gson.fromJson(msg, Tweet.class);
                    if (tweet.coordinates != null) {
                        //System.out.println(msg);
                        tweetServer.publish(gson.toJson(tweet));
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
