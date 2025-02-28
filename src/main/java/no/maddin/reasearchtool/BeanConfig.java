package no.maddin.reasearchtool;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

public interface BeanConfig {
    EmbeddingStore<TextSegment> embeddingStore();

    ChatLanguageModel chatModel();
}
