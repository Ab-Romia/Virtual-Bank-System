package com.virtualbank.assistant.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Retrieves the policy chunks most relevant to a question and renders them as the
 * grounding text the model must answer policy questions from. Retrieval uses the
 * local embedding model, so it works with no API key.
 */
@Component
public class PolicyRetriever {

    private final VectorStore vectorStore;

    public PolicyRetriever(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /** Returns the top matching policy chunks joined into one block, or empty if none match. */
    public String retrieve(String question) {
        List<Document> hits = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(3)
                        .build());
        if (hits == null || hits.isEmpty()) {
            return "";
        }
        return hits.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n---\n\n"));
    }
}
