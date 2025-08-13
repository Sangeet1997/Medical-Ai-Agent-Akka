package com.healthassistant.http;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.AskPattern;
import akka.http.javadsl.Http;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpEntities;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.healthassistant.messages.RouterMessages.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

/**
 * HTTP server for handling user queries
 */
public class HealthAssistantHttpServer extends AllDirectives {
    private static final Logger logger = LoggerFactory.getLogger(HealthAssistantHttpServer.class);

    private final ActorSystem<?> system;
    private final ActorRef<UserQuery> routerActor;

    public HealthAssistantHttpServer(ActorSystem<?> system, ActorRef<UserQuery> routerActor) {
        this.system = system;
        this.routerActor = routerActor;
    }

    // Request/Response DTOs
    public static class QueryRequest {
        public final String query;
        public final String userId;

        @JsonCreator
        public QueryRequest(@JsonProperty("query") String query, @JsonProperty("userId") String userId) {
            this.query = query;
            this.userId = userId != null ? userId : "anonymous";
        }
    }

    public static class QueryResponseDto {
        public final String response;
        public final String department;
        public final String queryId;
        public final boolean success;

        @JsonCreator
        public QueryResponseDto(
                @JsonProperty("response") String response,
                @JsonProperty("department") String department,
                @JsonProperty("queryId") String queryId,
                @JsonProperty("success") boolean success) {
            this.response = response;
            this.department = department;
            this.queryId = queryId;
            this.success = success;
        }
    }

    public Route createRoutes() {
        return concat(
                // Health check endpoint
                path("health", () ->
                        get(() ->
                                complete(StatusCodes.OK, "Health Assistant System is running")
                        )
                ),

                // Main query endpoint
                path("query", () ->
                        post(() ->
                                entity(Jackson.unmarshaller(QueryRequest.class), request ->
                                        onSuccess(processQuery(request), response ->
                                                complete(StatusCodes.OK, response, Jackson.marshaller())
                                        )
                                )
                        )
                ),

                // Static info endpoint
                path("info", () ->
                        get(() -> {
                            String jsonResponse = "{\"system\":\"Health Assistant\",\"version\":\"1.0.0\",\"departments\":[\"general-medicine\",\"pharmacy\",\"radiology\"]}";
                            return complete(StatusCodes.OK, HttpEntities.create(ContentTypes.APPLICATION_JSON, jsonResponse));
                        })
                ),

                // Catch all
                pathPrefix("", () ->
                        complete(StatusCodes.NOT_FOUND, "Endpoint not found. Available endpoints: /health, /query, /info")
                )
        );
    }

    private CompletionStage<QueryResponseDto> processQuery(QueryRequest request) {
        logger.info("Received HTTP query from user {}: {}", request.userId, request.query);

        // Create user query message
        CompletionStage<QueryResponse> responseFuture = AskPattern.ask(
                routerActor,
                replyTo -> new UserQuery(request.query, request.userId, replyTo),
                Duration.ofSeconds(180),
                system.scheduler()
        );

        return responseFuture.thenApply(response -> {
            logger.info("Returning HTTP response for query {}: success={}", 
                    response.queryId, response.success);
            
            return new QueryResponseDto(
                    response.response,
                    response.department,
                    response.queryId,
                    response.success
            );
        }).exceptionally(throwable -> {
            logger.error("Error processing query", throwable);
            return new QueryResponseDto(
                    "An error occurred while processing your request. Please try again.",
                    "error",
                    UUID.randomUUID().toString(),
                    false
            );
        });
    }

    public void startServer(String interface_, int port) {
        Http.get(system)
                .newServerAt(interface_, port)
                .bind(createRoutes())
                .thenAccept(binding -> {
                    logger.info("Health Assistant HTTP server started at http://{}:{}", 
                            interface_, port);
                    logger.info("Available endpoints:");
                    logger.info("  GET  http://{}:{}/health - Health check", interface_, port);
                    logger.info("  POST http://{}:{}/query - Submit medical query", interface_, port);
                    logger.info("  GET  http://{}:{}/info - System information", interface_, port);
                })
                .exceptionally(throwable -> {
                    logger.error("Failed to start HTTP server", throwable);
                    return null;
                });
    }
}
