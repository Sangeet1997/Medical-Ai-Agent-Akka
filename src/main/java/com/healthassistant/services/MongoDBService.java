package com.healthassistant.services;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Accumulators;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.healthassistant.messages.ChatHistoryMessages.ChatHistoryEntry;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * MongoDB service for chat history persistence
 */
public class MongoDBService {
    private static final Logger logger = LoggerFactory.getLogger(MongoDBService.class);
    
    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private final MongoCollection<Document> chatHistoryCollection;
    
    private static final String DATABASE_NAME = "health_assistant";
    private static final String COLLECTION_NAME = "chat_history";

    public MongoDBService(String connectionString) {
        try {
            this.mongoClient = MongoClients.create(connectionString);
            this.database = mongoClient.getDatabase(DATABASE_NAME);
            this.chatHistoryCollection = database.getCollection(COLLECTION_NAME);
            
            // Create indexes for better query performance
            createIndexes();
            
            logger.info("Connected to MongoDB database: {}", DATABASE_NAME);
        } catch (Exception e) {
            logger.error("Failed to connect to MongoDB", e);
            throw new RuntimeException("MongoDB connection failed", e);
        }
    }

    private void createIndexes() {
        try {
            // Index on userId for user-specific queries
            chatHistoryCollection.createIndex(new Document("userId", 1));
            
            // Index on timestamp for time-based queries
            chatHistoryCollection.createIndex(new Document("timestamp", -1));
            
            // Compound index for user + timestamp
            chatHistoryCollection.createIndex(new Document()
                .append("userId", 1)
                .append("timestamp", -1));
            
            // Index on department for department-specific analytics
            chatHistoryCollection.createIndex(new Document("department", 1));
            
            // Index on sessionId for session-based queries
            chatHistoryCollection.createIndex(new Document("sessionId", 1));
            
            logger.info("MongoDB indexes created successfully");
        } catch (Exception e) {
            logger.warn("Failed to create MongoDB indexes", e);
        }
    }

    /**
     * Save chat history entry to MongoDB
     */
    public CompletionStage<Boolean> saveChatHistory(ChatHistoryEntry entry) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Document document = new Document()
                    .append("queryId", entry.queryId)
                    .append("userId", entry.userId)
                    .append("query", entry.query)
                    .append("department", entry.department)
                    .append("response", entry.response)
                    .append("timestamp", Date.from(entry.timestamp.atZone(ZoneId.systemDefault()).toInstant()))
                    .append("success", entry.success)
                    .append("sessionId", entry.sessionId)
                    .append("responseTimeMs", entry.responseTimeMs);

                chatHistoryCollection.insertOne(document);
                
                logger.info("Saved chat history entry for user: {}, queryId: {}", 
                           entry.userId, entry.queryId);
                return true;
                
            } catch (Exception e) {
                logger.error("Failed to save chat history entry: {}", entry.queryId, e);
                return false;
            }
        });
    }

    /**
     * Get chat history for a user
     */
    public CompletionStage<List<ChatHistoryEntry>> getChatHistory(String userId, 
                                                                  String sessionId, 
                                                                  int limit, 
                                                                  String department) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Bson> filters = new ArrayList<>();
                filters.add(Filters.eq("userId", userId));
                
                if (sessionId != null && !sessionId.isEmpty()) {
                    filters.add(Filters.eq("sessionId", sessionId));
                }
                
                if (department != null && !department.isEmpty()) {
                    filters.add(Filters.eq("department", department));
                }

                Bson filter = filters.size() == 1 ? filters.get(0) : Filters.and(filters);
                
                FindIterable<Document> documents = chatHistoryCollection
                    .find(filter)
                    .sort(Sorts.descending("timestamp"))
                    .limit(limit);

                List<ChatHistoryEntry> entries = new ArrayList<>();
                for (Document doc : documents) {
                    ChatHistoryEntry entry = documentToChatHistoryEntry(doc);
                    entries.add(entry);
                }

                logger.info("Retrieved {} chat history entries for user: {}", entries.size(), userId);
                return entries;
                
            } catch (Exception e) {
                logger.error("Failed to get chat history for user: {}", userId, e);
                return new ArrayList<>();
            }
        });
    }

    /**
     * Get chat analytics
     */
    public CompletionStage<Map<String, Object>> getChatAnalytics(String userId, 
                                                                 LocalDateTime startDate, 
                                                                 LocalDateTime endDate) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Bson> matchFilters = new ArrayList<>();
                
                if (userId != null && !userId.isEmpty()) {
                    matchFilters.add(Filters.eq("userId", userId));
                }
                
                if (startDate != null) {
                    matchFilters.add(Filters.gte("timestamp", 
                        Date.from(startDate.atZone(ZoneId.systemDefault()).toInstant())));
                }
                
                if (endDate != null) {
                    matchFilters.add(Filters.lte("timestamp", 
                        Date.from(endDate.atZone(ZoneId.systemDefault()).toInstant())));
                }

                List<Bson> pipeline = new ArrayList<>();
                
                if (!matchFilters.isEmpty()) {
                    Bson matchStage = Aggregates.match(
                        matchFilters.size() == 1 ? matchFilters.get(0) : Filters.and(matchFilters)
                    );
                    pipeline.add(matchStage);
                }

                // Group and calculate analytics
                pipeline.add(Aggregates.group(null,
                    Accumulators.sum("totalQueries", 1),
                    Accumulators.sum("successfulQueries", 
                        new Document("$cond", Arrays.asList("$success", 1, 0))),
                    Accumulators.avg("averageResponseTime", "$responseTimeMs")
                ));

                AggregateIterable<Document> result = chatHistoryCollection.aggregate(pipeline);
                Document stats = result.first();

                Map<String, Object> analytics = new HashMap<>();
                if (stats != null) {
                    analytics.put("totalQueries", stats.getLong("totalQueries"));
                    analytics.put("successfulQueries", stats.getLong("successfulQueries"));
                    analytics.put("averageResponseTime", stats.getDouble("averageResponseTime"));
                } else {
                    analytics.put("totalQueries", 0L);
                    analytics.put("successfulQueries", 0L);
                    analytics.put("averageResponseTime", 0.0);
                }

                // Get most active user (if not filtering by specific user)
                if (userId == null || userId.isEmpty()) {
                    String mostActiveUser = getMostActiveUser(startDate, endDate);
                    analytics.put("mostActiveUser", mostActiveUser);
                }

                // Get most popular department
                String mostPopularDepartment = getMostPopularDepartment(userId, startDate, endDate);
                analytics.put("mostPopularDepartment", mostPopularDepartment);

                logger.info("Generated chat analytics: {}", analytics);
                return analytics;
                
            } catch (Exception e) {
                logger.error("Failed to get chat analytics", e);
                return new HashMap<>();
            }
        });
    }

    private String getMostActiveUser(LocalDateTime startDate, LocalDateTime endDate) {
        try {
            List<Bson> pipeline = new ArrayList<>();
            
            // Match date range if specified
            List<Bson> matchFilters = new ArrayList<>();
            if (startDate != null) {
                matchFilters.add(Filters.gte("timestamp", 
                    Date.from(startDate.atZone(ZoneId.systemDefault()).toInstant())));
            }
            if (endDate != null) {
                matchFilters.add(Filters.lte("timestamp", 
                    Date.from(endDate.atZone(ZoneId.systemDefault()).toInstant())));
            }
            
            if (!matchFilters.isEmpty()) {
                pipeline.add(Aggregates.match(
                    matchFilters.size() == 1 ? matchFilters.get(0) : Filters.and(matchFilters)
                ));
            }
            
            pipeline.add(Aggregates.group("$userId", Accumulators.sum("count", 1)));
            pipeline.add(Aggregates.sort(Sorts.descending("count")));
            pipeline.add(Aggregates.limit(1));

            AggregateIterable<Document> result = chatHistoryCollection.aggregate(pipeline);
            Document top = result.first();
            
            return top != null ? top.getString("_id") : "N/A";
            
        } catch (Exception e) {
            logger.error("Failed to get most active user", e);
            return "N/A";
        }
    }

    private String getMostPopularDepartment(String userId, LocalDateTime startDate, LocalDateTime endDate) {
        try {
            List<Bson> pipeline = new ArrayList<>();
            
            List<Bson> matchFilters = new ArrayList<>();
            if (userId != null && !userId.isEmpty()) {
                matchFilters.add(Filters.eq("userId", userId));
            }
            if (startDate != null) {
                matchFilters.add(Filters.gte("timestamp", 
                    Date.from(startDate.atZone(ZoneId.systemDefault()).toInstant())));
            }
            if (endDate != null) {
                matchFilters.add(Filters.lte("timestamp", 
                    Date.from(endDate.atZone(ZoneId.systemDefault()).toInstant())));
            }
            
            if (!matchFilters.isEmpty()) {
                pipeline.add(Aggregates.match(
                    matchFilters.size() == 1 ? matchFilters.get(0) : Filters.and(matchFilters)
                ));
            }
            
            pipeline.add(Aggregates.group("$department", Accumulators.sum("count", 1)));
            pipeline.add(Aggregates.sort(Sorts.descending("count")));
            pipeline.add(Aggregates.limit(1));

            AggregateIterable<Document> result = chatHistoryCollection.aggregate(pipeline);
            Document top = result.first();
            
            return top != null ? top.getString("_id") : "N/A";
            
        } catch (Exception e) {
            logger.error("Failed to get most popular department", e);
            return "N/A";
        }
    }

    private ChatHistoryEntry documentToChatHistoryEntry(Document doc) {
        Date timestamp = doc.getDate("timestamp");
        LocalDateTime localDateTime = timestamp.toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime();

        return new ChatHistoryEntry(
            doc.getString("queryId"),
            doc.getString("userId"),
            doc.getString("query"),
            doc.getString("department"),
            doc.getString("response"),
            localDateTime,
            doc.getBoolean("success", false),
            doc.getString("sessionId"),
            doc.getLong("responseTimeMs")
        );
    }

    /**
     * Close MongoDB connection
     */
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
            logger.info("MongoDB connection closed");
        }
    }

    /**
     * Check if MongoDB is connected
     */
    public boolean isConnected() {
        try {
            database.runCommand(new Document("ping", 1));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
