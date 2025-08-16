package com.healthassistant.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import com.healthassistant.messages.LLMMessages.*;
import com.healthassistant.messages.LoggerMessages.*;
import com.healthassistant.messages.RouterMessages.*;
import com.healthassistant.messages.ChatHistoryMessages.*;
import com.healthassistant.utils.DepartmentClassifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

/**
 * RadiologyActor handles imaging and radiology queries.
 * Demonstrates all three communication patterns in a single actor.
 */
public class RadiologyActor extends AbstractBehavior<Object> {
    private static final Logger logger = LoggerFactory.getLogger(RadiologyActor.class);

    private final ActorRef<Object> llmActor;
    private final ActorRef<Object> loggerActor;
    private final ActorRef<Object> chatHistoryActor;
    private final String nodeName;

    public static Behavior<Object> create() {
        return Behaviors.setup(RadiologyActor::new);
    }

    private RadiologyActor(ActorContext<Object> context) {
        super(context);
        
        // Create child actors
        this.llmActor = context.spawn(LLMActor.create(), "radiology-llm-actor");
        this.loggerActor = context.spawn(LoggerActor.create(), "radiology-logger-actor");
        this.chatHistoryActor = context.spawn(ChatHistoryActor.create(), "radiology-chat-history-actor");
        this.nodeName = context.getSystem().address().toString();
        
        logger.info("RadiologyActor started on node: {}", nodeName);
    }

    // Internal message for handling LLM responses
    private static class LLMResponseReceived {
        final RouteQuery originalQuery;
        final LLMResponse llmResponse;

        LLMResponseReceived(RouteQuery originalQuery, LLMResponse llmResponse) {
            this.originalQuery = originalQuery;
            this.llmResponse = llmResponse;
        }
    }

    @Override
    public Receive<Object> createReceive() {
        return newReceiveBuilder()
                .onMessage(RouteQuery.class, this::handleQuery)
                .onMessage(LLMResponseReceived.class, this::handleLLMResponse)
                .build();
    }

    private Behavior<Object> handleQuery(RouteQuery query) {
        logger.info("RadiologyActor processing query {} from user {} on node {} (multiple patterns)", 
                query.queryId, query.userId, nodeName);

        // Prepare radiology-specific context for LLM using utility
        String context = DepartmentClassifier.getDepartmentContext("radiology");

        // Create ProcessQuery message for LLM (ask pattern)
        ProcessQuery llmQuery = new ProcessQuery(query.query, context, query.queryId, 
                getContext().messageAdapter(LLMResponse.class, 
                        response -> new LLMResponseReceived(query, response)));

        // Using 'ask' pattern to get response from LLMActor
        llmActor.tell(llmQuery);
        
        logger.info("Sent radiology query {} to LLMActor using 'ask' pattern", query.queryId);

        return this;
    }

    private Behavior<Object> handleLLMResponse(LLMResponseReceived message) {
        RouteQuery originalQuery = message.originalQuery;
        LLMResponse llmResponse = message.llmResponse;
        
        logger.info("Received LLM response for radiology query {} (success: {})", 
                llmResponse.queryId, llmResponse.success);

        // Create response for user
        String responseText = llmResponse.success ? 
                llmResponse.response : 
                "I apologize, but I'm having trouble accessing radiology information right now. Please consult with a radiologist or your healthcare provider.";

        QueryResponse userResponse = new QueryResponse(
                responseText,
                "radiology",
                originalQuery.queryId,
                llmResponse.success
        );

        // Send response back to user immediately (tell pattern)
        originalQuery.replyTo.tell(userResponse);
        logger.info("Sent response to user using 'tell' pattern for query {}", originalQuery.queryId);

        // Create log entry and send to LoggerActor using 'tell' pattern (fire and forget)
        LogEntry logEntry = new LogEntry(
                originalQuery.queryId,
                originalQuery.userId,
                originalQuery.query,
                responseText,
                "radiology",
                LocalDateTime.now(),
                llmResponse.success
        );

        // Create chat history entry
        ChatHistoryEntry chatEntry = new ChatHistoryEntry(
                originalQuery.queryId,
                originalQuery.userId,
                originalQuery.query,
                responseText,
                "radiology",
                LocalDateTime.now(),
                llmResponse.success,
                "radiology-session-" + System.currentTimeMillis(),
                null // responseTimeMs - can be calculated later
        );

        // Save to chat history (fire-and-forget)
        chatHistoryActor.tell(new SaveChatHistoryFireAndForget(chatEntry));

        // Using 'tell' pattern for logging - fire and forget
        loggerActor.tell(logEntry);
        
        logger.info("Sent log entry for query {} to LoggerActor using 'tell' pattern", 
                originalQuery.queryId);
        logger.info("RadiologyActor completed processing query {} using multiple communication patterns", 
                originalQuery.queryId);

        return this;
    }
}
