package server;

import java.io.IOException;
import file.*;
import udp.*;
import admin.*;
import log.Logger;

public class ExtensionManager {

    private static FileHttpServer fileHttpServer;
    private static Thread udpBroadcastThread;
    private static UDPBroadcastServer udpBroadcastServer;
    private static AdminCommandHandler adminCommandHandler;

    public static void init() {
        Logger logger = Logger.getInstance();
        logger.info("ExtensionManager initialization started...");

        ChatServer chatServer = ChatServer.getInstance();
        if (chatServer == null) {
            logger.error("ChatServer instance is null. Please ensure ChatServer is initialized before calling ExtensionManager.init()");
            return;
        }

        try {
            fileHttpServer = new FileHttpServer();
            fileHttpServer.start();
            logger.info("FileHttpServer started on port 8080");
        } catch (IOException e) {
            logger.error("Failed to start FileHttpServer: " + e.getMessage());
        }

        try {
            udpBroadcastServer = new UDPBroadcastServer(chatServer);
            udpBroadcastThread = new Thread(udpBroadcastServer, "UDP-Broadcast-Thread");
            udpBroadcastThread.start();
            logger.info("UDPBroadcastServer started on port 9999");
        } catch (Exception e) {
            logger.error("Failed to start UDPBroadcastServer: " + e.getMessage());
        }

        try {
            adminCommandHandler = new AdminCommandHandler(chatServer);
            logger.info("AdminCommandHandler initialized");
        } catch (Exception e) {
            logger.error("Failed to initialize AdminCommandHandler: " + e.getMessage());
            e.printStackTrace();
        }

        logger.info("ExtensionManager initialization completed.");
    }

    public static void shutdown() {
        Logger logger = Logger.getInstance();
        logger.info("ExtensionManager shutting down...");

        if (udpBroadcastServer != null) {
            udpBroadcastServer.stop();
        }
        if (fileHttpServer != null) {
            fileHttpServer.stop();
        }
        if (udpBroadcastThread != null && udpBroadcastThread.isAlive()) {
            try {
                udpBroadcastThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        logger.info("ExtensionManager shutdown complete.");
    }

    public static AdminCommandHandler getAdminCommandHandler() {
        return adminCommandHandler;
    }
}