package no.maddin.reasearchtool;

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;

import java.io.Console;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static dev.langchain4j.model.chat.ChatLanguageModelExtensionsKt.chat;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

@Slf4j
public class Application implements CommandLineRunner {

    private final InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

    private final ChatLanguageModel chatModel = OpenAiChatModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .modelName(GPT_4_O_MINI)
        .build();

    private final Assistant assistant = AiServices.builder(Assistant.class)
        .chatLanguageModel(chatModel)
        .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
        .contentRetriever(EmbeddingStoreContentRetriever.from(embeddingStore))
        .build();

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        if (args.length > 0) {
            List<Document> docs = ingest(List.of(args));
            store(docs);

            String response;
            while((response = chat()) != null) {
                log.info("Response: {}", response);
            }
        } else {
            log.error("No arguments given");
        }

    }

    private void store(List<Document> docs) {
        EmbeddingStoreIngestor.ingest(docs, embeddingStore);
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
    private String chat() {
        try (Scanner inputScanner = new Scanner(System.in)) {

            System.out.print("Prompt: ");
            String message = inputScanner.nextLine();
            if (message == null || message.isEmpty()) {
                log.error("No Message, exiting");
                return null;
            }
            return assistant.chat(message);
        }
    }
}
