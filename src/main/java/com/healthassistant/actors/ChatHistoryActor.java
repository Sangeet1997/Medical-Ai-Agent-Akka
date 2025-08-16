package com.healthassistant.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.healthassistant.messages.ChatHistoryMessages.*;
import com.healthassistant.services.MongoDBService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ChatHistoryActor manages persistent chat history storage in MongoDB.
 * Works alongside LoggerActor to provide both in-memory and persistent storage.
 */
public class ChatHistoryActor extends AbstractBehavior<Object> {
    private static final Logger logger = LoggerFactory.getLogger(ChatHistoryActor.class);

    private final MongoDBService mongoDBService;
    private final String nodeName;

    public static Behavior<Object> create() {
        return Behaviors.setup(ChatHistoryActor::new);
    }

    private ChatHistoryActor(ActorContext<Object> context) {
        super(context);
        
        // Get MongoDB connection string from configuration
        String connectionString = context.getSystem().settings().config()
                .getString("health-assistant.mongodb.connection-string");
        
        this.mongoDBService = new MongoDBService(connectionString);
        this.nodeName = context.getSystem().address().toString();
        
        logger.info("ChatHistoryActor started on node: {} with MongoDB connection", nodeName);
        
        // Check MongoDB connectivity
        if (mongoDBService.isConnected()) {
            logger.info("✅ Successfully connected to MongoDB");
        } else {
            logger.warn("⚠️ MongoDB connection check failed - operating in degraded mode");
        }
    }

    @Override
    public Receive<Object> createReceive() {
        return newReceiveBuilder()
                .onMessage(SaveChatHistory.class, this::onSaveChatHistory)
                .onMessage(SaveChatHistoryFireAndForget.class, this::onSaveChatHistoryFireAndForget)
                .onMessage(GetChatHistory.class, this::onGetChatHistory)
                .onMessage(GetChatAnalytics.class, this::onGetChatAnalytics)
                .onMessage(ChatHistoryEntry.class, this::onDirectChatHistoryEntry)
                .build();
    }

    /**
     * Handle direct chat history entry (for backward compatibility with existing actors)
     */
    private Behavior<Object> onDirectChatHistoryEntry(ChatHistoryEntry entry) {
        logger.info("Received direct chat history entry for query: {}", entry.queryId);
        
        // Save to MongoDB without requiring a reply
        getContext().pipeToSelf(
                mongoDBService.saveChatHistory(entry),
                (result, throwable) -> {
                    if (throwable != null) {
                        logger.error("Failed to save chat history entry: {}", entry.queryId, throwable);
                        return new SaveChatHistoryResponse(entry.queryId, false, throwable.getMessage());
                    } else {
                        logger.debug("Successfully saved chat history entry: {}", entry.queryId);
                        return new SaveChatHistoryResponse(entry.queryId, result, null);
                    }
                }
        );

        return this;
    }

    /**
     * Handle save chat history request
     */
    private Behavior<Object> onSaveChatHistory(SaveChatHistory command) {
        logger.info("Saving chat history for query: {} from user: {}", 
                   command.entry.queryId, command.entry.userId);
        
        getContext().pipeToSelf(
                mongoDBService.saveChatHistory(command.entry),
                (result, throwable) -> {
                    SaveChatHistoryResponse response;
                    if (throwable != null) {
                        logger.error("Failed to save chat history: {}", command.entry.queryId, throwable);
                        response = SaveChatHistoryResponse.failure(command.entry.queryId, throwable.getMessage());
                    } else {
                        logger.debug("Successfully saved chat history: {}", command.entry.queryId);
                        response = SaveChatHistoryResponse.success(command.entry.queryId);
                    }
                    
                    command.replyTo.tell(response);
                    return response;
                }
        );

        return this;
    }

    /**
     * Handle save chat history request (fire-and-forget version)
     */
    private Behavior<Object> onSaveChatHistoryFireAndForget(SaveChatHistoryFireAndForget command) {
        logger.info("Saving chat history (fire-and-forget) for query: {} from user: {}", 
                   command.entry.queryId, command.entry.userId);
        
        getContext().pipeToSelf(
                mongoDBService.saveChatHistory(command.entry),
                (result, throwable) -> {
                    if (throwable != null) {
                        logger.error("Failed to save chat history: {}", command.entry.queryId, throwable);
                    } else {
                        logger.info("✅ Successfully saved chat history to MongoDB: {} for user: {}", 
                                   command.entry.queryId, command.entry.userId);
                    }
                    return new SaveChatHistoryResponse(command.entry.queryId, result != null, 
                                                      throwable != null ? throwable.getMessage() : null);
                }
        );

        return this;
    }

    /**
     * Handle get chat history request
     */
    private Behavior<Object> onGetChatHistory(GetChatHistory command) {
        logger.info("Retrieving chat history for user: {}, limit: {}", command.userId, command.limit);
        
        getContext().pipeToSelf(
                mongoDBService.getChatHistory(command.userId, command.sessionId, 
                                            command.limit, command.department),
                (entries, throwable) -> {
                    GetChatHistoryResponse response;
                    if (throwable != null) {
                        logger.error("Failed to get chat history for user: {}", command.userId, throwable);
                        response = GetChatHistoryResponse.failure(command.userId, throwable.getMessage());
                    } else {
                        logger.info("Retrieved {} chat history entries for user: {}", 
                                   entries.size(), command.userId);
                        response = GetChatHistoryResponse.success(command.userId, entries);
                    }
                    
                    command.replyTo.tell(response);
                    return response;
                }
        );

        return this;
    }

    /**
     * Handle get chat analytics request
     */
    private Behavior<Object> onGetChatAnalytics(GetChatAnalytics command) {
        logger.info("Generating chat analytics for user: {}, period: {} to {}", 
                   command.userId, command.startDate, command.endDate);
        
        getContext().pipeToSelf(
                mongoDBService.getChatAnalytics(command.userId, command.startDate, command.endDate),
                (analytics, throwable) -> {
                    GetChatAnalyticsResponse response;
                    if (throwable != null) {
                        logger.error("Failed to get chat analytics", throwable);
                        response = GetChatAnalyticsResponse.failure(throwable.getMessage());
                    } else {
                        long totalQueries = (Long) analytics.getOrDefault("totalQueries", 0L);
                        long successfulQueries = (Long) analytics.getOrDefault("successfulQueries", 0L);
                        double avgResponseTime = (Double) analytics.getOrDefault("averageResponseTime", 0.0);
                        String mostActiveUser = (String) analytics.getOrDefault("mostActiveUser", "N/A");
                        String mostPopularDepartment = (String) analytics.getOrDefault("mostPopularDepartment", "N/A");
                        
                        response = new GetChatAnalyticsResponse(
                            totalQueries, successfulQueries, avgResponseTime,
                            mostActiveUser, mostPopularDepartment, true, null
                        );
                        
                        logger.info("Generated analytics: {} total queries, {} successful, avg response time: {}ms", 
                                   totalQueries, successfulQueries, avgResponseTime);
                    }
                    
                    command.replyTo.tell(response);
                    return response;
                }
        );

        return this;
    }

    /**
     * Create a convenience method to generate session ID
     */
    public static String generateSessionId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Create ChatHistoryEntry from query processing data
     */
    public static ChatHistoryEntry createChatHistoryEntry(String queryId, String userId, 
                                                         String query, String department, 
                                                         String response, boolean success, 
                                                         String sessionId, Long responseTimeMs) {
        return new ChatHistoryEntry(
            queryId,
            userId,
            query,
            department,
            response,
            LocalDateTime.now(),
            success,
            sessionId != null ? sessionId : generateSessionId(),
            responseTimeMs
        );
    }

    public Behavior<Object> onSignal(akka.actor.typed.Signal signal) {
        if (signal instanceof akka.actor.typed.Terminated) {
            logger.info("ChatHistoryActor terminated, closing MongoDB connection");
            mongoDBService.close();
        }
        return this;
    }
}
