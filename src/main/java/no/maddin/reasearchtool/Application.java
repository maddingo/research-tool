package no.maddin.reasearchtool;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class Application implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        if (args.length > 0) {
            ingest(List.of(args));
        } else {
            log.error("No arguments given");
        }

    }

    /**
     * follow https://docs.langchain4j.dev/tutorials/rag
     */
    private void ingest(List<String> files) {
        List<Document> documents = new ArrayList<>();
        files.forEach(file -> documents.add(FileSystemDocumentLoader.loadDocument(file)));
        log.info("Loaded {} documents", documents.size());
    }
}
