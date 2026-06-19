package com.virtualbank.assistant.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Embeds the bank-policy corpus into the vector store once, after the context is
 * ready. It only loads when the store is empty, so a restart against the shared
 * Postgres does not re-embed the same text. The corpus is a small Markdown file
 * split on its top-level headings, which keeps each policy topic in one chunk.
 */
@Component
public class PolicyCorpusLoader {

    private static final Logger log = LoggerFactory.getLogger(PolicyCorpusLoader.class);

    private final VectorStore vectorStore;
    private final Resource corpus;

    public PolicyCorpusLoader(
            VectorStore vectorStore,
            @Value("classpath:policies/bank-policies.md") Resource corpus) {
        this.vectorStore = vectorStore;
        this.corpus = corpus;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadIfEmpty() {
        if (alreadyLoaded()) {
            log.info("Policy corpus already present in the vector store; skipping embedding");
            return;
        }
        List<Document> documents = readChunks();
        vectorStore.add(documents);
        log.info("Embedded {} policy chunks into the vector store", documents.size());
    }

    /**
     * The store is considered loaded if any policy document is already present.
     * A similarity search is enough here and stays on the VectorStore abstraction
     * rather than reaching into the backing table.
     */
    private boolean alreadyLoaded() {
        try {
            return !vectorStore.similaritySearch(
                    org.springframework.ai.vectorstore.SearchRequest.builder()
                            .query("bank policy")
                            .topK(1)
                            .build())
                    .isEmpty();
        } catch (RuntimeException e) {
            // If the probe fails (for example the table is not ready yet) treat the
            // store as empty and let the add attempt surface any real problem.
            return false;
        }
    }

    private List<Document> readChunks() {
        String text = read(corpus);
        List<Document> documents = new ArrayList<>();
        for (String section : text.split("(?m)^#\\s")) {
            String chunk = section.strip();
            if (chunk.isEmpty()) {
                continue;
            }
            String title = chunk.lines().findFirst().orElse("policy").strip();
            documents.add(new Document(chunk, Map.of("source", "bank-policies", "section", title)));
        }
        return documents;
    }

    private static String read(Resource resource) {
        try {
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read the policy corpus", e);
        }
    }
}
