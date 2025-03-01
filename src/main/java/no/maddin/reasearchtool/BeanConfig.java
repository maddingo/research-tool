package no.maddin.reasearchtool;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;

public interface BeanConfig {
    EmbeddingStore<TextSegment> embeddingStore();

    ChatMemory chatMemory();

    ContentRetriever contentRetriever(EmbeddingStore<TextSegment> embeddingStore);

    ChatLanguageModel chatModel();

    RetrievalAugmentor retrievalAugmentor(ContentRetriever contentRetriever);
}
