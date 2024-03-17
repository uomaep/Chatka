package com.uomaep.chat.config;

import akka.actor.ActorSystem;
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


    public static void loadRedisConfig(ActorSystem system) {
        Config config = system.settings().config();
        redisIP = config.getString("akka.environment." + envType + ".redis-ip");
        redisPort = config.getString("akka.environment." + envType + ".redis-port");
    }

}

