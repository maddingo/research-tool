package no.maddin.reasearchtool;

import dev.langchain4j.data.document.BlankDocumentException;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.IngestionResult;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Slf4j
@RequiredArgsConstructor
public class DocumentIngester {
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    public void addDocuments(List<String> files) {
        log.info("Ingesting {} files", files.size());
        executor.submit(() -> {
            IngestionResult inResult = ingest(files);
            log.info("Ingestion Result: total tokens={}", inResult.tokenUsage().totalTokenCount());
        });
    }

    @PreDestroy
    private void close() {
        List<Runnable> unfinishedTasks = executor.shutdownNow();
        log.info("Shutting down executor with {} unfinished tasks", unfinishedTasks.size());
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
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
            .embeddingStore(embeddingStore)
            .textSegmentTransformer(ts -> {
                log.debug("TextSegment transformer: {}", ts.metadata());
                var fileName = Optional.ofNullable(ts.metadata())
                    .filter(md -> md.containsKey("absolute_directory_path") && md.containsKey("file_name"))
                    .map(md ->
                        md.getString("absolute_directory_path") + File.separator + md.getString("file_name")
                    )
                    .orElse("");

                return TextSegment.from(
                    ts.text(),
                    ts.metadata().put("source", fileName)
                );}
            )
            .build();
        return ingestor.ingest(documents);
    }
}
