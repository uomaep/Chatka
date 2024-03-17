package com.uomaep.chat.config;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.http.javadsl.model.HttpResponse;
import com.typesafe.config.Config;

public class Environment {
    private static String envType = "dev";
    private static String serviceMode = "running";
    private static String hostName = "localhost";
    private static String redisIP;
    private static String redisPort;
    private static int port = 8000;

    public static void setEnvType(String str) {
        envType = str;
    }

    public static String getEnvType() {
        return envType;
    }

    public static void setServiceMode(String mode) {
        serviceMode = mode;
    }

    public static String getServiceMode() {
        return serviceMode;
    }

    public static void setHostName(String host) {
        hostName = host;
    }

    public static String getHostName() {
        return hostName;
    }

    public static void setPort(int portNum) {
        port = portNum;
    }

    public static int getPort() {
        return port;
    }

    public static String getRedisIP() {
        return redisIP;
    }

    public static String getRedisPort() {
        return redisPort;
    }


    public static void loadRedisConfig(ActorSystem system) {
        Config config = system.settings().config();
        redisIP = config.getString("akka.environment." + envType + ".redis-ip");
        redisPort = config.getString("akka.environment." + envType + ".redis-port");
    }

    // HttpClient
    public static class HttpClientGet {
        public final String event;
        public final String path;
        public HttpClientGet(String event, String path) {
            this.event = event;
            this.path = path;
        }
    }
    public static class HttpClientPost {
        public final String event;
        public final String path;
        public final String token;
        public final String jsonBody;

        public HttpClientPost(String event, String path, String token, String jsonBody) {
            this.event = event;
            this.path = path;
            this.token = token;
            this.jsonBody = jsonBody;
        }
    }
    public static class HttpClientResponseSuccess {
        public final String event;
        public final HttpResponse httpResponse;
        public final ActorRef recipient;

        public HttpClientResponseSuccess(String event, HttpResponse httpResponse, ActorRef recipient) {
            this.event = event;
            this.httpResponse = httpResponse;
            this.recipient = recipient;
        }
    }
    public static class HttpClientResponseFailure {
        public final String event;
        public final String reason;
        public final ActorRef recipient;

        public HttpClientResponseFailure(String event, String reason, ActorRef recipient) {
            this.event = event;
            this.reason = reason;
            this.recipient = recipient;
        }
    }
}

