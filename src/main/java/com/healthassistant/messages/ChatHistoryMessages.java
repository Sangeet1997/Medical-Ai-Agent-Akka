package com.healthassistant.messages;

import akka.actor.typed.ActorRef;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Messages for MongoDB Chat History Actor communication
 */
public class ChatHistoryMessages {

    /**
     * Chat history entry for MongoDB storage
     */
    public static final class ChatHistoryEntry implements CborSerializable {
        public final String queryId;
        public final String userId;
        public final String query;
        public final String department;
        public final String response;
        public final LocalDateTime timestamp;
        public final boolean success;
        public final String sessionId; // For grouping conversations
        public final Long responseTimeMs; // Response time tracking

        @JsonCreator
        public ChatHistoryEntry(
                @JsonProperty("queryId") String queryId,
                @JsonProperty("userId") String userId,
                @JsonProperty("query") String query,
                @JsonProperty("department") String department,
                @JsonProperty("response") String response,
                @JsonProperty("timestamp") LocalDateTime timestamp,
                @JsonProperty("success") boolean success,
                @JsonProperty("sessionId") String sessionId,
                @JsonProperty("responseTimeMs") Long responseTimeMs) {
            this.queryId = queryId;
            this.userId = userId;
            this.query = query;
            this.department = department;
            this.response = response;
            this.timestamp = timestamp;
            this.success = success;
            this.sessionId = sessionId;
            this.responseTimeMs = responseTimeMs;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ChatHistoryEntry that = (ChatHistoryEntry) o;
            return success == that.success &&
                    Objects.equals(queryId, that.queryId) &&
                    Objects.equals(userId, that.userId) &&
                    Objects.equals(query, that.query) &&
                    Objects.equals(department, that.department) &&
                    Objects.equals(response, that.response) &&
                    Objects.equals(timestamp, that.timestamp) &&
                    Objects.equals(sessionId, that.sessionId) &&
                    Objects.equals(responseTimeMs, that.responseTimeMs);
        }

        @Override
        public int hashCode() {
            return Objects.hash(queryId, userId, query, department, response, 
                              timestamp, success, sessionId, responseTimeMs);
        }

        @Override
        public String toString() {
            return "ChatHistoryEntry{" +
                    "queryId='" + queryId + '\'' +
                    ", userId='" + userId + '\'' +
                    ", query='" + query + '\'' +
                    ", department='" + department + '\'' +
                    ", response='" + response + '\'' +
                    ", timestamp=" + timestamp +
                    ", success=" + success +
                    ", sessionId='" + sessionId + '\'' +
                    ", responseTimeMs=" + responseTimeMs +
                    '}';
        }
    }

    /**
     * Request to save chat history entry to MongoDB
     */
    public static final class SaveChatHistory implements CborSerializable {
        public final ChatHistoryEntry entry;
        public final ActorRef<SaveChatHistoryResponse> replyTo;

        @JsonCreator
        public SaveChatHistory(
                @JsonProperty("entry") ChatHistoryEntry entry,
                @JsonProperty("replyTo") ActorRef<SaveChatHistoryResponse> replyTo) {
            this.entry = entry;
            this.replyTo = replyTo;
        }

        @Override
        public String toString() {
            return "SaveChatHistory{" +
                    "entry=" + entry +
                    ", replyTo=" + replyTo +
                    '}';
        }
    }

    /**
     * Fire-and-forget version of SaveChatHistory (no response needed)
     */
    public static final class SaveChatHistoryFireAndForget implements CborSerializable {
        public final ChatHistoryEntry entry;

        @JsonCreator
        public SaveChatHistoryFireAndForget(@JsonProperty("entry") ChatHistoryEntry entry) {
            this.entry = entry;
        }

        @Override
        public String toString() {
            return "SaveChatHistoryFireAndForget{" +
                    "entry=" + entry +
                    '}';
        }
    }

    /**
     * Response for save chat history operation
     */
    public static final class SaveChatHistoryResponse implements CborSerializable {
        public final String queryId;
        public final boolean success;
        public final String errorMessage;

        @JsonCreator
        public SaveChatHistoryResponse(
                @JsonProperty("queryId") String queryId,
                @JsonProperty("success") boolean success,
                @JsonProperty("errorMessage") String errorMessage) {
            this.queryId = queryId;
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public static SaveChatHistoryResponse success(String queryId) {
            return new SaveChatHistoryResponse(queryId, true, null);
        }

        public static SaveChatHistoryResponse failure(String queryId, String errorMessage) {
            return new SaveChatHistoryResponse(queryId, false, errorMessage);
        }

        @Override
        public String toString() {
            return "SaveChatHistoryResponse{" +
                    "queryId='" + queryId + '\'' +
                    ", success=" + success +
                    ", errorMessage='" + errorMessage + '\'' +
                    '}';
        }
    }

    /**
     * Request to get chat history for a user
     */
    public static final class GetChatHistory implements CborSerializable {
        public final String userId;
        public final String sessionId; // Optional - null means all sessions
        public final int limit;
        public final String department; // Optional filter
        public final ActorRef<GetChatHistoryResponse> replyTo;

        @JsonCreator
        public GetChatHistory(
                @JsonProperty("userId") String userId,
                @JsonProperty("sessionId") String sessionId,
                @JsonProperty("limit") int limit,
                @JsonProperty("department") String department,
                @JsonProperty("replyTo") ActorRef<GetChatHistoryResponse> replyTo) {
            this.userId = userId;
            this.sessionId = sessionId;
            this.limit = limit;
            this.department = department;
            this.replyTo = replyTo;
        }

        @Override
        public String toString() {
            return "GetChatHistory{" +
                    "userId='" + userId + '\'' +
                    ", sessionId='" + sessionId + '\'' +
                    ", limit=" + limit +
                    ", department='" + department + '\'' +
                    ", replyTo=" + replyTo +
                    '}';
        }
    }

    /**
     * Response containing chat history
     */
    public static final class GetChatHistoryResponse implements CborSerializable {
        public final String userId;
        public final List<ChatHistoryEntry> entries;
        public final boolean success;
        public final String errorMessage;

        @JsonCreator
        public GetChatHistoryResponse(
                @JsonProperty("userId") String userId,
                @JsonProperty("entries") List<ChatHistoryEntry> entries,
                @JsonProperty("success") boolean success,
                @JsonProperty("errorMessage") String errorMessage) {
            this.userId = userId;
            this.entries = entries;
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public static GetChatHistoryResponse success(String userId, List<ChatHistoryEntry> entries) {
            return new GetChatHistoryResponse(userId, entries, true, null);
        }

        public static GetChatHistoryResponse failure(String userId, String errorMessage) {
            return new GetChatHistoryResponse(userId, null, false, errorMessage);
        }

        @Override
        public String toString() {
            return "GetChatHistoryResponse{" +
                    "userId='" + userId + '\'' +
                    ", entriesCount=" + (entries != null ? entries.size() : 0) +
                    ", success=" + success +
                    ", errorMessage='" + errorMessage + '\'' +
                    '}';
        }
    }

    /**
     * Request to get chat analytics/statistics
     */
    public static final class GetChatAnalytics implements CborSerializable {
        public final String userId; // Optional - null means system-wide
        public final LocalDateTime startDate;
        public final LocalDateTime endDate;
        public final ActorRef<GetChatAnalyticsResponse> replyTo;

        @JsonCreator
        public GetChatAnalytics(
                @JsonProperty("userId") String userId,
                @JsonProperty("startDate") LocalDateTime startDate,
                @JsonProperty("endDate") LocalDateTime endDate,
                @JsonProperty("replyTo") ActorRef<GetChatAnalyticsResponse> replyTo) {
            this.userId = userId;
            this.startDate = startDate;
            this.endDate = endDate;
            this.replyTo = replyTo;
        }

        @Override
        public String toString() {
            return "GetChatAnalytics{" +
                    "userId='" + userId + '\'' +
                    ", startDate=" + startDate +
                    ", endDate=" + endDate +
                    ", replyTo=" + replyTo +
                    '}';
        }
    }

    /**
     * Analytics response
     */
    public static final class GetChatAnalyticsResponse implements CborSerializable {
        public final long totalQueries;
        public final long successfulQueries;
        public final double averageResponseTime;
        public final String mostActiveUser;
        public final String mostPopularDepartment;
        public final boolean success;
        public final String errorMessage;

        @JsonCreator
        public GetChatAnalyticsResponse(
                @JsonProperty("totalQueries") long totalQueries,
                @JsonProperty("successfulQueries") long successfulQueries,
                @JsonProperty("averageResponseTime") double averageResponseTime,
                @JsonProperty("mostActiveUser") String mostActiveUser,
                @JsonProperty("mostPopularDepartment") String mostPopularDepartment,
                @JsonProperty("success") boolean success,
                @JsonProperty("errorMessage") String errorMessage) {
            this.totalQueries = totalQueries;
            this.successfulQueries = successfulQueries;
            this.averageResponseTime = averageResponseTime;
            this.mostActiveUser = mostActiveUser;
            this.mostPopularDepartment = mostPopularDepartment;
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public static GetChatAnalyticsResponse failure(String errorMessage) {
            return new GetChatAnalyticsResponse(0, 0, 0, null, null, false, errorMessage);
        }

        @Override
        public String toString() {
            return "GetChatAnalyticsResponse{" +
                    "totalQueries=" + totalQueries +
                    ", successfulQueries=" + successfulQueries +
                    ", averageResponseTime=" + averageResponseTime +
                    ", mostActiveUser='" + mostActiveUser + '\'' +
                    ", mostPopularDepartment='" + mostPopularDepartment + '\'' +
                    ", success=" + success +
                    ", errorMessage='" + errorMessage + '\'' +
                    '}';
        }
    }
}
