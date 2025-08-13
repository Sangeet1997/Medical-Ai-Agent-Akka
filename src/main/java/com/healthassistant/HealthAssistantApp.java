package com.healthassistant;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import akka.cluster.typed.Cluster;
import akka.cluster.typed.ClusterSingleton;
import akka.cluster.typed.SingletonActor;
import com.healthassistant.actors.RouterActor;
import com.healthassistant.http.HealthAssistantHttpServer;
import com.healthassistant.messages.RouterMessages.UserQuery;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main application class for the Distributed Health Assistant System
 */
public class HealthAssistantApp {
    private static final Logger logger = LoggerFactory.getLogger(HealthAssistantApp.class);

    public static void main(String[] args) {
        // Determine which configuration to use based on arguments
        String configName = "application";
        if (args.length > 0) {
            configName = args[0];
        }

        logger.info("Starting Health Assistant System with config: {}", configName);

        // Load configuration
        Config config = ConfigFactory.load(configName);
        
        // Create actor system
        ActorSystem<Void> system = ActorSystem.create(
                createMainBehavior(),
                "HealthAssistantSystem",
                config
        );

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down Health Assistant System...");
            system.terminate();
        }));

        logger.info("Health Assistant System started successfully");
    }

    private static Behavior<Void> createMainBehavior() {
        return Behaviors.setup(context -> {
            // Get cluster instance
            Cluster cluster = Cluster.get(context.getSystem());
            
            logger.info("=== CLUSTER NODE STARTING ===");
            logger.info("Node address: {}", cluster.selfMember().address());
            logger.info("Node roles: {}", cluster.selfMember().getRoles());
            logger.info("Cluster seed nodes: {}", context.getSystem().settings().config().getStringList("akka.cluster.seed-nodes"));
            logger.info("============================");

            // Create cluster singleton router
            ClusterSingleton singleton = ClusterSingleton.get(context.getSystem());
            ActorRef<UserQuery> routerActor = singleton.init(SingletonActor.of(
                    RouterActor.create(),
                    "health-assistant-router"
            ));

            logger.info("Health Assistant Router singleton created");

            // Start HTTP server
            startHttpServer(context.getSystem(), routerActor, context.getSystem().settings().config());

            // Register cluster event listeners
            registerClusterEventListeners(cluster, context.getSystem());

            return Behaviors.empty();
        });
    }

    private static void startHttpServer(ActorSystem<?> system, ActorRef<UserQuery> routerActor, Config config) {
        try {
            String interface_ = config.getString("health-assistant.http-server.interface");
            int port = config.getInt("health-assistant.http-server.port");

            HealthAssistantHttpServer httpServer = new HealthAssistantHttpServer(system, routerActor);
            httpServer.startServer(interface_, port);
            
            logger.info("HTTP server configuration - Interface: {}, Port: {}", interface_, port);
        } catch (Exception e) {
            logger.error("Failed to start HTTP server", e);
        }
    }

    private static void registerClusterEventListeners(Cluster cluster, ActorSystem<?> system) {
        logger.info("Cluster event listeners would be registered here in a full implementation");
        // Note: Simplified for this demo - in a full implementation you would register
        // proper cluster event listeners here
    }
}
