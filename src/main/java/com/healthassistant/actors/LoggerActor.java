package com.healthassistant.actors;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.healthassistant.messages.LoggerMessages.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * LoggerActor handles logging of all queries and responses.
 * Demonstrates 'tell' pattern for fire-and-forget logging operations.
 */
public class LoggerActor extends AbstractBehavior<Object> {
    private static final Logger logger = LoggerFactory.getLogger(LoggerActor.class);

    private final List<LogEntry> logEntries;
    private final String nodeName;

    public static Behavior<Object> create() {
        return Behaviors.setup(LoggerActor::new);
    }

    private LoggerActor(ActorContext<Object> context) {
        super(context);
        this.logEntries = new ArrayList<>();
        this.nodeName = context.getSystem().address().toString();
        
        logger.info("LoggerActor started on node: {}", nodeName);
    }

    @Override
    public Receive<Object> createReceive() {
        return newReceiveBuilder()
                .onMessage(LogEntry.class, this::logEntry)
                .onMessage(GetLogEntries.class, this::getLogEntries)
                .onMessage(PharmacyActor.ForwardToLogger.class, this::handleForward)
                .build();
    }

    private Behavior<Object> logEntry(LogEntry entry) {
        // Using 'tell' pattern - this is a fire-and-forget operation
        logger.info("Logging entry for query {} from user {} to {} department (tell pattern)", 
                entry.queryId, entry.userId, entry.department);
        
        // Store log entry
        logEntries.add(entry);
        
        // Log detailed information
        logger.info("=== QUERY LOG ===");
        logger.info("Query ID: {}", entry.queryId);
        logger.info("User ID: {}", entry.userId);
        logger.info("Department: {}", entry.department);
        logger.info("Query: {}", entry.query);
        logger.info("Response: {}", entry.response != null ? 
                (entry.response.length() > 100 ? entry.response.substring(0, 100) + "..." : entry.response) : 
                "No response");
        logger.info("Success: {}", entry.success);
        logger.info("Timestamp: {}", entry.timestamp);
        logger.info("Node: {}", nodeName);
        logger.info("=================");
        
        // Keep only last 1000 entries to prevent memory issues
        if (logEntries.size() > 1000) {
            logEntries.remove(0);
        }

        return this;
    }

    private Behavior<Object> getLogEntries(GetLogEntries request) {
        logger.info("Retrieving log entries for user: {}, limit: {}", request.userId, request.limit);
        
        List<LogEntry> userEntries = logEntries.stream()
                .filter(entry -> request.userId == null || request.userId.equals(entry.userId))
                .sorted((a, b) -> b.timestamp.compareTo(a.timestamp)) // Most recent first
                .limit(request.limit > 0 ? request.limit : 10)
                .collect(Collectors.toList());
        
        logger.info("Found {} log entries for user {}", userEntries.size(), request.userId);
        
        // In a real system, you would return this data to the requester
        // For this demo, we just log the summary
        userEntries.forEach(entry -> 
                logger.info("Entry: {} - {} - {} - Success: {}", 
                        entry.timestamp, entry.department, 
                        entry.query.length() > 50 ? entry.query.substring(0, 50) + "..." : entry.query,
                        entry.success));

        return this;
    }

    public int getLogEntryCount() {
        return logEntries.size();
    }

    /**
     * Handle forward pattern - log the entry and forward response to original sender
     */
    private Behavior<Object> handleForward(PharmacyActor.ForwardToLogger forwardMessage) {
        logger.info("FORWARD PATTERN: LoggerActor received forward request for query {} - " +
                "will log and forward to original sender", 
                forwardMessage.logEntry.queryId);

        // First, log the entry
        logEntry(forwardMessage.logEntry);

        // Then forward the response to the original sender (maintaining sender context)
        forwardMessage.originalSender.tell(forwardMessage.response);
        
        logger.info("FORWARD PATTERN: Successfully logged and forwarded response for query {} " +
                "to original sender {}", 
                forwardMessage.logEntry.queryId, forwardMessage.originalSender);

        return this;
    }
}
