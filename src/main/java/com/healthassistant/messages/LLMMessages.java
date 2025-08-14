package com.healthassistant.messages;

import akka.actor.typed.ActorRef;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Messages for LLM Actor communication
 */
public class LLMMessages {

    /**
     * Request to process a query with LLM
     */
    public static final class ProcessQuery implements CborSerializable {
        public final String query;
        public final String context;
        public final String queryId;
        public final ActorRef<LLMResponse> replyTo;

        @JsonCreator
        public ProcessQuery(
                @JsonProperty("query") String query,
                @JsonProperty("context") String context,
                @JsonProperty("queryId") String queryId,
                @JsonProperty("replyTo") ActorRef<LLMResponse> replyTo) {
            this.query = query;
            this.context = context;
            this.queryId = queryId;
            this.replyTo = replyTo;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ProcessQuery that = (ProcessQuery) o;
            return Objects.equals(query, that.query) &&
                    Objects.equals(context, that.context) &&
                    Objects.equals(queryId, that.queryId) &&
                    Objects.equals(replyTo, that.replyTo);
        }

        @Override
        public int hashCode() {
            return Objects.hash(query, context, queryId, replyTo);
        }

        @Override
        public String toString() {
            return "ProcessQuery{" +
                    "query='" + query + '\'' +
                    ", context='" + context + '\'' +
                    ", queryId='" + queryId + '\'' +
                    ", replyTo=" + replyTo +
                    '}';
        }
    }

    /**
     * Response from LLM processing
     */
    public static final class LLMResponse implements CborSerializable {
        public final String response;
        public final String queryId;
        public final boolean success;
        public final String errorMessage;

        @JsonCreator
        public LLMResponse(
                @JsonProperty("response") String response,
                @JsonProperty("queryId") String queryId,
                @JsonProperty("success") boolean success,
                @JsonProperty("errorMessage") String errorMessage) {
            this.response = response;
            this.queryId = queryId;
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public static LLMResponse success(String response, String queryId) {
            return new LLMResponse(response, queryId, true, null);
        }

        public static LLMResponse failure(String errorMessage, String queryId) {
            return new LLMResponse(null, queryId, false, errorMessage);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LLMResponse that = (LLMResponse) o;
            return success == that.success &&
                    Objects.equals(response, that.response) &&
                    Objects.equals(queryId, that.queryId) &&
                    Objects.equals(errorMessage, that.errorMessage);
        }

        @Override
        public int hashCode() {
            return Objects.hash(response, queryId, success, errorMessage);
        }

        @Override
        public String toString() {
            return "LLMResponse{" +
                    "response='" + response + '\'' +
                    ", queryId='" + queryId + '\'' +
                    ", success=" + success +
                    ", errorMessage='" + errorMessage + '\'' +
                    '}';
        }
    }
}
