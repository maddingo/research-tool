package no.maddin.reasearchtool;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.IngestionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;

@Slf4j
@SpringBootApplication
public class Application implements CommandLineRunner {

    private final EmbeddingStore<TextSegment> embeddingStore;

    private final Assistant assistant;

    public Application(EmbeddingStore<TextSegment> embeddingStore, ChatLanguageModel chatModel) {
        this.embeddingStore = embeddingStore;
        this.assistant = AiServices.builder(Assistant.class)
                .chatLanguageModel(chatModel)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .contentRetriever(EmbeddingStoreContentRetriever.from(embeddingStore))
                .build();
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        if (args.length > 0) {
            List<Document> docs = ingest(List.of(args));
            IngestionResult inResult = store(docs);
            log.info("Ingestion Result: {}", inResult);

            chat(line -> log.info("Response: {}", line));
        } else {
            log.error("No arguments given");
        }

    }

    private IngestionResult store(List<Document> docs) {
        return EmbeddingStoreIngestor.ingest(docs, embeddingStore);
    }

    /**
     * follow https://docs.langchain4j.dev/tutorials/rag
     */
    private List<Document> ingest(List<String> files) {
        List<Document> documents = new ArrayList<>();
        files.forEach(file -> documents.add(FileSystemDocumentLoader.loadDocument(file)));
        log.info("Loaded {} documents", documents.size());

        return documents;
    }

    @SuppressWarnings("java:S106")
    private void chat(Consumer<String> lineConsumer) {
        try (Scanner inputScanner = new Scanner(System.in)) {
            while(true) {
                System.out.print("Prompt: ");
                String message = inputScanner.nextLine();
                if (message == null || message.isEmpty()) {
                    log.error("No Message, exiting");
                    return;
                }
                assistant.chat(message);
            }
        }
    }
}
