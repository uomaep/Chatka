package com.uomaep;

import akka.actor.ActorSystem;
import com.typesafe.config.ConfigFactory;
import com.uomaep.chat.service.AdminService;
import com.uomaep.chat.service.ChatService;
import com.uomaep.chat.service.HealthyService;
import scala.concurrent.ExecutionContext;

public class Main {
    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("chatka", ConfigFactory.load());
        ExecutionContext executionContext = system.getDispatcher();

        String env = environmentSelector(system, args);

        HealthyService healthyService = new HealthyService(system);
        AdminService adminService = new AdminService(system, healthyService);
        ChatService chatService = new ChatService(env, system, adminService);

        healthyService.start();
        adminService.start();
        chatService.start();
    }

    public static String environmentSelector(ActorSystem system, String[] args) {
        if(args.length != 1) {
            showUsageAndTerminate(system);
            return null;
        }

        if(args[0].equals("service") || args[0].equals("dev")) {
            return args[0];
        } else {
            showUsageAndTerminate(system);
            return null;
        }
    }

    private static void showUsageAndTerminate(ActorSystem system) {
        System.out.println("\nUsage: ");
        System.out.println("mvn exec:java -Dexec.mainClass=\"Main\" -Dexec.args=\"[service|dev]\"\n");
        system.terminate();
    }
}