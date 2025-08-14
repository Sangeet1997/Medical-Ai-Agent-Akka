package com.healthassistant.actors;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.healthassistant.messages.LLMMessages.*;
import com.healthassistant.services.OllamaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * LLMActor handles communication with LLaMA model via Ollama.
 * Demonstrates 'ask' pattern for async request-response communication.
 */
public class LLMActor extends AbstractBehavior<Object> {
    private static final Logger logger = LoggerFactory.getLogger(LLMActor.class);

    private final OllamaService ollamaService;
    private final String nodeName;

    public static Behavior<Object> create() {
        return Behaviors.setup(LLMActor::new);
    }

    private LLMActor(ActorContext<Object> context) {
        super(context);
        
        // Initialize Ollama service with configuration
        String baseUrl = context.getSystem().settings().config()
                .getString("health-assistant.ollama.base-url");
        String model = context.getSystem().settings().config()
                .getString("health-assistant.ollama.model");
        Duration timeout = context.getSystem().settings().config()
                .getDuration("health-assistant.ollama.timeout");

        this.ollamaService = new OllamaService(context.getSystem(), baseUrl, model, timeout);
        this.nodeName = context.getSystem().address().toString();
        
        logger.info("LLMActor started on node: {} with Ollama URL: {}, model: {}", 
                nodeName, baseUrl, model);
        
        // Check Ollama availability on startup
        checkOllamaAvailability();
    }

    // Internal message for wrapping the LLM response with original query
    private static class WrappedLLMResponse {
        final ProcessQuery originalQuery;
        final LLMResponse response;

        WrappedLLMResponse(ProcessQuery originalQuery, LLMResponse response) {
            this.originalQuery = originalQuery;
            this.response = response;
        }
    }

    @Override
    public Receive<Object> createReceive() {
        return newReceiveBuilder()
                .onMessage(ProcessQuery.class, this::processQuery)
                .onMessage(WrappedLLMResponse.class, this::handleWrappedResponse)
                .build();
    }

    private Behavior<Object> processQuery(ProcessQuery query) {
        logger.info("Processing query {} on node {} using LLaMA model (ask pattern)", 
                query.queryId, nodeName);

        // Async processing using CompletionStage - demonstrates non-blocking actor behavior
        getContext().pipeToSelf(
                ollamaService.generateResponse(query.query, query.context),
                (response, throwable) -> {
                    if (throwable != null) {
                        logger.error("LLM processing failed for query {}", query.queryId, throwable);
                        return new WrappedLLMResponse(query, LLMResponse.failure(
                                "Failed to process query: " + throwable.getMessage(),
                                query.queryId
                        ));
                    } else {
                        logger.info("LLM processing successful for query {}, response length: {}", 
                                query.queryId, response.length());
                        return new WrappedLLMResponse(query, LLMResponse.success(response, query.queryId));
                    }
                }
        );

        return this;
    }

    private Behavior<Object> handleWrappedResponse(WrappedLLMResponse wrapped) {
        // Send response back to requester
        wrapped.originalQuery.replyTo.tell(wrapped.response);
        return this;
    }

    private void checkOllamaAvailability() {
        getContext().pipeToSelf(
                ollamaService.isAvailable(),
                (available, throwable) -> {
                    if (available != null && available) {
                        logger.info("Ollama service is available and ready");
                    } else {
                        logger.warn("Ollama service is not available. Please ensure Ollama is running with LLaMA model loaded.");
                    }
                    return available != null ? available : false;
                }
        );
    }
}
