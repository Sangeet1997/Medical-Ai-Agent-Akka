package com.healthassistant.services;

import akka.actor.typed.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.*;
import akka.stream.javadsl.Sink;
import akka.util.ByteString;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Service for communicating with Ollama LLaMA model
 */
public class OllamaService {
    private static final Logger logger = LoggerFactory.getLogger(OllamaService.class);
    
    private final Http http;
    private final ActorSystem<?> system;
    private final String baseUrl;
    private final String model;
    private final Duration timeout;
    private final ObjectMapper objectMapper;

    public OllamaService(ActorSystem<?> system, String baseUrl, String model, Duration timeout) {
        this.system = system;
        this.http = Http.get(system);
        this.baseUrl = baseUrl;
        this.model = model;
        this.timeout = timeout;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Generate a response using the LLaMA model
     */
    public CompletionStage<String> generateResponse(String prompt, String context) {
        String enhancedPrompt = buildEnhancedPrompt(prompt, context);
        
        logger.info("Sending request to Ollama: model={}, prompt length={}", model, enhancedPrompt.length());
        
        return createOllamaRequest(enhancedPrompt)
                .thenCompose(this::sendRequest)
                .thenCompose(this::parseResponse)
                .exceptionally(throwable -> {
                    logger.error("Error communicating with Ollama", throwable);
                    return "I apologize, but I'm currently unable to process your request due to a technical issue. Please try again later.";
                });
    }

    private String buildEnhancedPrompt(String prompt, String context) {
        StringBuilder enhancedPrompt = new StringBuilder();
        
        enhancedPrompt.append("You are a helpful medical assistant. ");
        
        if (context != null && !context.isEmpty()) {
            enhancedPrompt.append("Context: ").append(context).append("\\n\\n");
        }
        
        enhancedPrompt.append("Please provide a helpful, accurate, and professional response to the following medical query. ");
        enhancedPrompt.append("Important: Always recommend consulting with healthcare professionals for serious medical concerns.\\n\\n");
        enhancedPrompt.append("Query: ").append(prompt);
        
        return enhancedPrompt.toString();
    }

    private CompletionStage<HttpRequest> createOllamaRequest(String prompt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Create request body for Ollama generate API
                String requestBody = objectMapper.writeValueAsString(
                    objectMapper.createObjectNode()
                        .put("model", model)
                        .put("prompt", prompt)
                        .put("stream", false)
                        .put("options", objectMapper.createObjectNode()
                            .put("temperature", 0.7)
                            .put("top_p", 0.9)
                            .put("max_tokens", 500))
                );

                return HttpRequest.POST(baseUrl + "/api/generate")
                        .withEntity(ContentTypes.APPLICATION_JSON, requestBody);
                        
            } catch (Exception e) {
                throw new RuntimeException("Failed to create Ollama request", e);
            }
        });
    }

    private CompletionStage<HttpResponse> sendRequest(HttpRequest request) {
        return http.singleRequest(request);
    }

    private CompletionStage<String> parseResponse(HttpResponse response) {
        if (response.status().isSuccess()) {
            return response.entity()
                    .toStrict(timeout.toMillis(), system)
                    .thenApply(entity -> {
                        try {
                            String responseBody = entity.getData().utf8String();
                            JsonNode jsonResponse = objectMapper.readTree(responseBody);
                            
                            if (jsonResponse.has("response")) {
                                String generatedText = jsonResponse.get("response").asText();
                                logger.info("Successfully received response from Ollama, length: {}", generatedText.length());
                                return generatedText;
                            } else {
                                logger.warn("Unexpected response format from Ollama: {}", responseBody);
                                return "I received an unexpected response format. Please try again.";
                            }
                        } catch (Exception e) {
                            logger.error("Failed to parse Ollama response", e);
                            return "I encountered an error while processing the response. Please try again.";
                        }
                    });
        } else {
            logger.error("Ollama request failed with status: {} {}", response.status().intValue(), response.status().reason());
            return CompletableFuture.completedFuture(
                "I'm currently experiencing difficulties. Please ensure Ollama is running and try again."
            );
        }
    }

    /**
     * Check if Ollama service is available
     */
    public CompletionStage<Boolean> isAvailable() {
        HttpRequest request = HttpRequest.GET(baseUrl + "/api/tags");
        
        return http.singleRequest(request)
                .thenApply(response -> {
                    boolean available = response.status().isSuccess();
                    logger.info("Ollama availability check: {}", available ? "Available" : "Unavailable");
                    return available;
                })
                .exceptionally(throwable -> {
                    logger.warn("Ollama availability check failed", throwable);
                    return false;
                });
    }
}
