package no.maddin.reasearchtool;

import dev.langchain4j.data.document.BlankDocumentException;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.IngestionResult;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class Application implements CommandLineRunner {

    private final EmbeddingStore<TextSegment> embeddingStore;

    private final Assistant assistant;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        if (args.length > 0) {
            IngestionResult inResult = ingest(List.of(args));
            log.info("Ingestion Result: total tokens={}", inResult.tokenUsage().totalTokenCount());

            chat(line -> log.info("Response: {}", line));
        } else {
            log.error("No arguments given");
        }

    }

    /**
     * follow https://docs.langchain4j.dev/tutorials/rag
     */
    private IngestionResult ingest(List<String> files) {
        List<Document> documents = new ArrayList<>();
        files.forEach(file -> {
            log.info("ingesting file {}", file);
            try {
                documents.add(FileSystemDocumentLoader.loadDocument(file));
            } catch (BlankDocumentException e) {
                log.warn("Error ingesting file {}", file, e);
            }
        });
        return EmbeddingStoreIngestor.ingest(documents, embeddingStore);
    }

    @SuppressWarnings("java:S106")
    private void chat(Consumer<String> lineConsumer) {
        try (Scanner inputScanner = new Scanner(System.in)) {
            while(true) {
                System.out.print("Prompt: ");
                String message = inputScanner.nextLine();
                if (message == null || message.isEmpty()) {
                    log.warn("No Message, exiting");
                    return;
                }
                String response = assistant.chat(message);
                lineConsumer.accept(response);
            }
        }
    }
}
