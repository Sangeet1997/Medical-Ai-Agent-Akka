package com.healthassistant.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Messages for Logger Actor communication
 */
public class LoggerMessages {

    /**
     * Log entry message
     */
    public static final class LogEntry implements CborSerializable {
        public final String queryId;
        public final String userId;
        public final String query;
        public final String response;
        public final String department;
        public final LocalDateTime timestamp;
        public final boolean success;

        @JsonCreator
        public LogEntry(
                @JsonProperty("queryId") String queryId,
                @JsonProperty("userId") String userId,
                @JsonProperty("query") String query,
                @JsonProperty("response") String response,
                @JsonProperty("department") String department,
                @JsonProperty("timestamp") LocalDateTime timestamp,
                @JsonProperty("success") boolean success) {
            this.queryId = queryId;
            this.userId = userId;
            this.query = query;
            this.response = response;
            this.department = department;
            this.timestamp = timestamp;
            this.success = success;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LogEntry logEntry = (LogEntry) o;
            return success == logEntry.success &&
                    Objects.equals(queryId, logEntry.queryId) &&
                    Objects.equals(userId, logEntry.userId) &&
                    Objects.equals(query, logEntry.query) &&
                    Objects.equals(response, logEntry.response) &&
                    Objects.equals(department, logEntry.department) &&
                    Objects.equals(timestamp, logEntry.timestamp);
        }

        @Override
        public int hashCode() {
            return Objects.hash(queryId, userId, query, response, department, timestamp, success);
        }

        @Override
        public String toString() {
            return "LogEntry{" +
                    "queryId='" + queryId + '\'' +
                    ", userId='" + userId + '\'' +
                    ", query='" + query + '\'' +
                    ", response='" + response + '\'' +
                    ", department='" + department + '\'' +
                    ", timestamp=" + timestamp +
                    ", success=" + success +
                    '}';
        }
    }

    /**
     * Request to retrieve log entries
     */
    public static final class GetLogEntries implements CborSerializable {
        public final String userId;
        public final int limit;

        @JsonCreator
        public GetLogEntries(
                @JsonProperty("userId") String userId,
                @JsonProperty("limit") int limit) {
            this.userId = userId;
            this.limit = limit;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GetLogEntries that = (GetLogEntries) o;
            return limit == that.limit &&
                    Objects.equals(userId, that.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, limit);
        }

        @Override
        public String toString() {
            return "GetLogEntries{" +
                    "userId='" + userId + '\'' +
                    ", limit=" + limit +
                    '}';
        }
    }
}
