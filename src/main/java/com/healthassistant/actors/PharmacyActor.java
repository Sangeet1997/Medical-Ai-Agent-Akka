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
 * PharmacyActor handles medication and pharmaceutical queries.
 * Demonstrates 'forward' pattern - processes message but maintains original sender.
 */
public class PharmacyActor extends AbstractBehavior<Object> {
    private static final Logger logger = LoggerFactory.getLogger(PharmacyActor.class);

    private final ActorRef<Object> llmActor;
    private final ActorRef<Object> loggerActor;
    private final ActorRef<Object> chatHistoryActor;
    private final String nodeName;

    public static Behavior<Object> create() {
        return Behaviors.setup(PharmacyActor::new);
    }

    private PharmacyActor(ActorContext<Object> context) {
        super(context);
        
        // Create child actors
        this.llmActor = context.spawn(LLMActor.create(), "pharmacy-llm-actor");
        this.loggerActor = context.spawn(LoggerActor.create(), "pharmacy-logger-actor");
        this.chatHistoryActor = context.spawn(ChatHistoryActor.create(), "pharmacy-chat-history-actor");
        this.nodeName = context.getSystem().address().toString();
        
        logger.info("PharmacyActor started on node: {}", nodeName);
    }

    // Internal message for handling LLM responses with forward pattern
    private static class LLMResponseForForward {
        final RouteQuery originalQuery;
        final LLMResponse llmResponse;

        LLMResponseForForward(RouteQuery originalQuery, LLMResponse llmResponse) {
            this.originalQuery = originalQuery;
            this.llmResponse = llmResponse;
        }
    }

    @Override
    public Receive<Object> createReceive() {
        return newReceiveBuilder()
                .onMessage(RouteQuery.class, this::handleQuery)
                .onMessage(LLMResponseForForward.class, this::handleLLMResponseForForward)
                .build();
    }

    private Behavior<Object> handleQuery(RouteQuery query) {
        logger.info("PharmacyActor processing query {} from user {} on node {} (forward pattern)", 
                query.queryId, query.userId, nodeName);

        // Prepare pharmacy-specific context for LLM using utility
        String context = DepartmentClassifier.getDepartmentContext("pharmacy");

        // Create ProcessQuery message for LLM
        ProcessQuery llmQuery = new ProcessQuery(query.query, context, query.queryId, 
                getContext().messageAdapter(LLMResponse.class, 
                        response -> new LLMResponseForForward(query, response)));

        // Using 'ask' pattern to get response from LLMActor
        llmActor.tell(llmQuery);
        
        logger.info("Sent pharmacy query {} to LLMActor for processing", query.queryId);

        return this;
    }

    private Behavior<Object> handleLLMResponseForForward(LLMResponseForForward message) {
        RouteQuery originalQuery = message.originalQuery;
        LLMResponse llmResponse = message.llmResponse;
        
        logger.info("Received LLM response for pharmacy query {} (success: {})", 
                llmResponse.queryId, llmResponse.success);

        // Create response for user
        String responseText = llmResponse.success ? 
                llmResponse.response : 
                "I apologize, but I'm having trouble accessing medication information right now. Please consult with a pharmacist.";

        QueryResponse userResponse = new QueryResponse(
                responseText,
                "pharmacy",
                originalQuery.queryId,
                llmResponse.success
        );

        // Create log entry for forwarding
        LogEntry logEntry = new LogEntry(
                originalQuery.queryId,
                originalQuery.userId,
                originalQuery.query,
                responseText,
                "pharmacy",
                LocalDateTime.now(),
                llmResponse.success
        );

        // Create chat history entry
        ChatHistoryEntry chatEntry = new ChatHistoryEntry(
                originalQuery.queryId,
                originalQuery.userId,
                originalQuery.query,
                responseText,
                "pharmacy",
                LocalDateTime.now(),
                llmResponse.success,
                "pharmacy-session-" + System.currentTimeMillis(),
                null // responseTimeMs - can be calculated later
        );

        // Save to chat history (fire-and-forget)
        chatHistoryActor.tell(new SaveChatHistoryFireAndForget(chatEntry));

        // FORWARD PATTERN: Send the response via LoggerActor while maintaining original sender
        // The LoggerActor will log and then forward the response to the original sender
        ForwardToLogger forwardMessage = new ForwardToLogger(userResponse, logEntry, originalQuery.replyTo);
        loggerActor.tell(forwardMessage);
        
        logger.info("FORWARD PATTERN: Forwarded pharmacy query {} response through LoggerActor " +
                "while maintaining original sender {}", 
                originalQuery.queryId, originalQuery.replyTo);

        return this;
    }

    /**
     * Message for forwarding response through LoggerActor
     */
    public static class ForwardToLogger {
        public final QueryResponse response;
        public final LogEntry logEntry;
        public final ActorRef<QueryResponse> originalSender;

        public ForwardToLogger(QueryResponse response, LogEntry logEntry, ActorRef<QueryResponse> originalSender) {
            this.response = response;
            this.logEntry = logEntry;
            this.originalSender = originalSender;
        }
    }
}
