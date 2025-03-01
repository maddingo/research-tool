package no.maddin.reasearchtool;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

public interface BeanConfig {
    EmbeddingStore<TextSegment> embeddingStore();

    @Bean
    ChatMemory chatMemory();

    @Bean
    ContentRetriever contentRetriever(EmbeddingStore<TextSegment> embeddingStore);

    ChatLanguageModel chatModel();
}
