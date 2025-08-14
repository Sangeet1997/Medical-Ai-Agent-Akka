package com.healthassistant.messages;

import akka.actor.typed.ActorRef;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Messages for Router Actor communication
 */
public class RouterMessages {

    /**
     * User query message sent to the RouterActor
     */
    public static final class UserQuery implements CborSerializable {
        public final String query;
        public final String userId;
        public final ActorRef<QueryResponse> replyTo;

        @JsonCreator
        public UserQuery(
                @JsonProperty("query") String query,
                @JsonProperty("userId") String userId,
                @JsonProperty("replyTo") ActorRef<QueryResponse> replyTo) {
            this.query = query;
            this.userId = userId;
            this.replyTo = replyTo;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UserQuery userQuery = (UserQuery) o;
            return Objects.equals(query, userQuery.query) &&
                    Objects.equals(userId, userQuery.userId) &&
                    Objects.equals(replyTo, userQuery.replyTo);
        }

        @Override
        public int hashCode() {
            return Objects.hash(query, userId, replyTo);
        }

        @Override
        public String toString() {
            return "UserQuery{" +
                    "query='" + query + '\'' +
                    ", userId='" + userId + '\'' +
                    ", replyTo=" + replyTo +
                    '}';
        }
    }

    /**
     * Query response sent back to the user
     */
    public static final class QueryResponse implements CborSerializable {
        public final String response;
        public final String department;
        public final String queryId;
        public final boolean success;

        @JsonCreator
        public QueryResponse(
                @JsonProperty("response") String response,
                @JsonProperty("department") String department,
                @JsonProperty("queryId") String queryId,
                @JsonProperty("success") boolean success) {
            this.response = response;
            this.department = department;
            this.queryId = queryId;
            this.success = success;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            QueryResponse that = (QueryResponse) o;
            return success == that.success &&
                    Objects.equals(response, that.response) &&
                    Objects.equals(department, that.department) &&
                    Objects.equals(queryId, that.queryId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(response, department, queryId, success);
        }

        @Override
        public String toString() {
            return "QueryResponse{" +
                    "response='" + response + '\'' +
                    ", department='" + department + '\'' +
                    ", queryId='" + queryId + '\'' +
                    ", success=" + success +
                    '}';
        }
    }

    /**
     * Message to route query to specific department
     */
    public static final class RouteQuery implements CborSerializable {
        public final String query;
        public final String userId;
        public final String queryId;
        public final ActorRef<QueryResponse> replyTo;

        @JsonCreator
        public RouteQuery(
                @JsonProperty("query") String query,
                @JsonProperty("userId") String userId,
                @JsonProperty("queryId") String queryId,
                @JsonProperty("replyTo") ActorRef<QueryResponse> replyTo) {
            this.query = query;
            this.userId = userId;
            this.queryId = queryId;
            this.replyTo = replyTo;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RouteQuery that = (RouteQuery) o;
            return Objects.equals(query, that.query) &&
                    Objects.equals(userId, that.userId) &&
                    Objects.equals(queryId, that.queryId) &&
                    Objects.equals(replyTo, that.replyTo);
        }

        @Override
        public int hashCode() {
            return Objects.hash(query, userId, queryId, replyTo);
        }

        @Override
        public String toString() {
            return "RouteQuery{" +
                    "query='" + query + '\'' +
                    ", userId='" + userId + '\'' +
                    ", queryId='" + queryId + '\'' +
                    ", replyTo=" + replyTo +
                    '}';
        }
    }
}
