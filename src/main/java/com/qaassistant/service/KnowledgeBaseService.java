package com.qaassistant.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeBaseService {

    private final VectorStore vectorStore;

    @Value("${app.rag.top-k:5}")
    private int topK;

    @PostConstruct
    public void loadKnowledgeBase() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] markdownResources = resolver.getResources("classpath:knowledge-base/*.md");
        List<Document> documents = new ArrayList<>();

        for (Resource resource : markdownResources) {
            MarkdownDocumentReader reader = new MarkdownDocumentReader(
                    resource,
                    MarkdownDocumentReaderConfig.defaultConfig()
            );
            documents.addAll(reader.get());
            log.info("Loaded knowledge base file: {}", resource.getFilename());
        }

        Resource pdfResource = resolver.getResource("classpath:knowledge-base/qa-guidelines.pdf");
        if (pdfResource.exists()) {
            try {
                org.springframework.ai.reader.pdf.PagePdfDocumentReader pdfReader =
                        new org.springframework.ai.reader.pdf.PagePdfDocumentReader(pdfResource);
                documents.addAll(pdfReader.get());
                log.info("Loaded knowledge base PDF: qa-guidelines.pdf");
            } catch (Exception e) {
                log.warn("Could not load qa-guidelines.pdf: {}", e.getMessage());
            }
        }

        if (!documents.isEmpty()) {
            vectorStore.add(documents);
            log.info("Indexed {} document chunks into vector store", documents.size());
        } else {
            log.warn("No knowledge base documents found to index");
        }
    }

    public String retrieveRelevantGuidance(String requirement, String implementationSummary) {
        String query = requirement + "\n" + implementationSummary;
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(topK).build()
        );

        if (results.isEmpty()) {
            return "No specific QA guidance retrieved. Apply general testing best practices.";
        }

        return results.stream()
                .map(doc -> {
                    String source = doc.getMetadata().getOrDefault("source", "unknown").toString();
                    return "### " + source + "\n" + doc.getText();
                })
                .collect(Collectors.joining("\n\n"));
    }
}
