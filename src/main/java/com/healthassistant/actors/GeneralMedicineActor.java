package com.healthassistant.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import com.healthassistant.messages.LLMMessages.*;
import com.healthassistant.messages.LoggerMessages.*;
import com.healthassistant.messages.RouterMessages.*;
import com.healthassistant.utils.DepartmentClassifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

/**
 * GeneralMedicineActor handles general medical queries.
 * Demonstrates 'ask' pattern with LLMActor and 'tell' pattern with LoggerActor.
 */
public class GeneralMedicineActor extends AbstractBehavior<Object> {
    private static final Logger logger = LoggerFactory.getLogger(GeneralMedicineActor.class);

    private final ActorRef<Object> llmActor;
    private final ActorRef<Object> loggerActor;
    private final String nodeName;

    public static Behavior<Object> create() {
        return Behaviors.setup(GeneralMedicineActor::new);
    }

    private GeneralMedicineActor(ActorContext<Object> context) {
        super(context);
        
        // Create child actors
        this.llmActor = context.spawn(LLMActor.create(), "llm-actor");
        this.loggerActor = context.spawn(LoggerActor.create(), "logger-actor");
        this.nodeName = context.getSystem().address().toString();
        
        logger.info("GeneralMedicineActor started on node: {}", nodeName);
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
        logger.info("GeneralMedicineActor processing query {} from user {} on node {} (ask pattern)", 
                query.queryId, query.userId, nodeName);

        // Prepare context for LLM using utility
        String context = DepartmentClassifier.getDepartmentContext("general-medicine");

        // Create ProcessQuery message for LLM
        ProcessQuery llmQuery = new ProcessQuery(query.query, context, query.queryId, 
                getContext().messageAdapter(LLMResponse.class, 
                        response -> new LLMResponseReceived(query, response)));

        // Using 'ask' pattern to get response from LLMActor
        llmActor.tell(llmQuery);
        
        logger.info("Sent query {} to LLMActor using 'ask' pattern", query.queryId);

        return this;
    }

    private Behavior<Object> handleLLMResponse(LLMResponseReceived message) {
        RouteQuery originalQuery = message.originalQuery;
        LLMResponse llmResponse = message.llmResponse;
        
        logger.info("Received LLM response for query {} (success: {})", 
                llmResponse.queryId, llmResponse.success);

        // Create response for user
        String responseText = llmResponse.success ? 
                llmResponse.response : 
                "I apologize, but I'm having trouble processing your request right now. Please try again later.";

        QueryResponse userResponse = new QueryResponse(
                responseText,
                "general-medicine",
                originalQuery.queryId,
                llmResponse.success
        );

        // Send response back to user
        originalQuery.replyTo.tell(userResponse);

        // Create log entry and send to LoggerActor using 'tell' pattern
        LogEntry logEntry = new LogEntry(
                originalQuery.queryId,
                originalQuery.userId,
                originalQuery.query,
                responseText,
                "general-medicine",
                LocalDateTime.now(),
                llmResponse.success
        );

        // Using 'tell' pattern for logging - fire and forget
        loggerActor.tell(logEntry);
        
        logger.info("Sent log entry for query {} to LoggerActor using 'tell' pattern", 
                originalQuery.queryId);
        logger.info("Completed processing query {} - responded to user", originalQuery.queryId);

        return this;
    }
}
