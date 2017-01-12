package com.attvn.adtruevn.util;

import android.util.Log;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by app on 12/17/16.
 */

public final class Logging {
    private static final String TAG = "Logging";
    private static final String USER = "test";
    private static final String PASS = "test";

    private static ConnectionFactory factory = null;
    private static String mLogName;

    private static void setupConnectionFactory() {
        String url = "amqp://"+ USER +":"+ PASS +"@"+ Params.IP_AMQP_SERVER +":5672/%2F";
        factory = new ConnectionFactory();
        try {
            factory.setAutomaticRecoveryEnabled(true);
            factory.setUri(url);
        } catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void connectToLogServer(String logName) {
        mLogName = logName;
        if(factory == null) {
            setupConnectionFactory();
        }
        publishToAMQPServer();
    }

    private static ContentLogBuilder mDefaultContentLogBuilder = ContentLogBuilder.buildDefault();

    public static void log(String message) {
        String loggingMessage = mDefaultContentLogBuilder.setContent(message).toString();
        sendToRemoteServer(loggingMessage);
    }

    private static BlockingDeque<String> queue = new LinkedBlockingDeque<String>();
    private static Thread publishThread;

    private synchronized static void publishToAMQPServer() {
        publishThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {
                        Connection connection = factory.newConnection();
                        Channel ch = connection.createChannel();
                        ch.confirmSelect();

                        while (true) {
                            String message = queue.takeFirst();
                            try {
                                ch.basicPublish("amq.direct", "logging_" + mLogName, null, message.getBytes());
                                Log.d(TAG, "[s] " + message);
                                ch.waitForConfirmsOrDie();
                            } catch (Exception e) {
                                Log.d(TAG, "[f] " + message);
                                queue.putFirst(message);
                                throw e;
                            }
                        }
                    } catch (InterruptedException e) {
                        break;
                    } catch (Exception e){
                        Log.d(TAG, "Connection: broken: " + e.getClass().getName() + " Host: " + factory.getHost());

                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e1) {
                            break;
                        }
                    }
                }
            }
        });
        publishThread.start();
    }

    private synchronized static void sendToRemoteServer(String sendMessage) {
//        // when connection is lost
//        if(factory == null || !publishThread.isAlive()) {
//            connectToLogServer();
//        }
        queue.addFirst(sendMessage);
    }

    private static void destroy() {
        if(publishThread != null) {
            publishThread.interrupt();
        }
        factory = null;
    }

    static class ContentLogBuilder {
        private String time;
        private String content;
        private SimpleDateFormat dateFormat;

        private ContentLogBuilder() {
            dateFormat = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
        }

        public ContentLogBuilder setContent(String s){
            Date now = new Date();
            time = dateFormat.format(now);
            content = s;
            return this;
        }

        @Override
        public String toString() {
            return "["+ time +"]: " + (content == null ? "" : content);
        }

        public static ContentLogBuilder buildDefault() {
            return new ContentLogBuilder();
        }
    }
}
