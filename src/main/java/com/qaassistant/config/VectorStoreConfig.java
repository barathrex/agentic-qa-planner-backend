package com.qaassistant.config;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Configuration
public class VectorStoreConfig {

    @Bean
    public VectorStore vectorStore() {
        return new VectorStore() {
            private final List<Document> documents = new ArrayList<>();

            @Override
            public void add(List<Document> docs) {
                documents.addAll(docs);
            }

            @Override
            public Optional<Boolean> delete(List<String> idList) {
                boolean removed = documents.removeIf(doc -> idList.contains(doc.getId()));
                return Optional.of(removed);
            }

            @Override
            public Optional<Boolean> delete(Filter.Expression filterExpression) {
                return Optional.of(false);
            }

            @Override
            public List<Document> similaritySearch(String query) {
                return similaritySearch(SearchRequest.builder().query(query).topK(5).build());
            }

            @Override
            public List<Document> similaritySearch(SearchRequest request) {
                if (documents.isEmpty()) {
                    return List.of();
                }
                String queryLower = request.getQuery() == null ? "" : request.getQuery().toLowerCase();
                List<String> keywords = Arrays.stream(queryLower.split("\\W+"))
                        .filter(w -> w.length() > 3)
                        .collect(Collectors.toList());

                return documents.stream()
                        .sorted((d1, d2) -> {
                            long c1 = keywords.stream().filter(k -> d1.getText().toLowerCase().contains(k)).count();
                            long c2 = keywords.stream().filter(k -> d2.getText().toLowerCase().contains(k)).count();
                            return Long.compare(c2, c1);
                        })
                        .limit(request.getTopK())
                        .collect(Collectors.toList());
            }
        };
    }
}
