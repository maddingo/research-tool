package no.maddin.reasearchtool;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Image;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.router.DefaultQueryRouter;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.PreDestroy;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Configuration
@Profile("dev")
@Slf4j
public class DevBeansConfig implements BeanConfig {

    private final ApplicationContext applicationContext;
    private final AtomicReference<OllamaContainer> container = new AtomicReference<>();
    private final String modelName;
    private final String dockerImageNameTC;
    private final String ollamaImage;

    public DevBeansConfig(
        @Autowired ApplicationContext applicationContext,
        @Value("${research-tool.ollama.image}") String ollamaImage,
        @Value("${research-tool.ollama.model}") String modelName
    ) {
        this.applicationContext = applicationContext;
        this.ollamaImage = ollamaImage;
        this.modelName = modelName;
        this.dockerImageNameTC = String.format("tc-%s-%s", ollamaImage, modelName);
    }

    @Bean
    @Override
    public EmbeddingStore<TextSegment> embeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }

    @Bean
    @Override
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.withMaxMessages(10);
    }

    @Bean
    @Override
    public ContentRetriever contentRetriever(@Autowired EmbeddingStore<TextSegment> embeddingStore) {
        return EmbeddingStoreContentRetriever.from(embeddingStore);
    }

    @Bean
    public OllamaContainer ollamaContainer() {
        return container.updateAndGet(c -> {
            if (c == null) {
                return createOllamaContainer();
            }
            return c;
        });
    }

    @Bean
    @Override
    public ChatModel chatModel() {
        OllamaContainer ollama = applicationContext.getBean(OllamaContainer.class);
        return OllamaChatModel.builder()
            .baseUrl(ollama.getEndpoint())
            .temperature(0.0)
            .logRequests(true)
            .logResponses(true)
            .modelName(modelName)
            .build();
    }

    @Bean
    @Override
    public RetrievalAugmentor retrievalAugmentor(@Autowired ContentRetriever contentRetriever) {
        return DefaultRetrievalAugmentor.builder()
            .contentInjector(DefaultContentInjector.builder()
                .promptTemplate(PromptTemplate.from("{{userMessage}}\n{{contents}}"))
//                .metadataKeysToInclude(List.of("absolute_directory_path", "file_name"))
                .metadataKeysToInclude(List.of("source"))
                .build())
            .queryRouter(new DefaultQueryRouter(contentRetriever))
            .build();
    }

    @PreDestroy
    private void stopContainer() {
        container.getAndUpdate(c -> {
            if (c != null) {
                c.stop();
            }
            return null;
        });
    }

    @SneakyThrows
    private OllamaContainer createOllamaContainer() {

        DockerImageName dockerImageName = DockerImageName.parse(ollamaImage);
        DockerClient dockerClient = DockerClientFactory.instance().client();
        List<Image> images = dockerClient.listImagesCmd().withReferenceFilter(dockerImageNameTC).exec();
        OllamaContainer ollama;
        if (images.isEmpty()) {
            ollama = new OllamaContainer(dockerImageName);
        } else {
            ollama = new OllamaContainer(DockerImageName.parse(dockerImageNameTC).asCompatibleSubstituteFor(ollamaImage));
        }
        ollama.start();

        // Pull the model and create an image based on the selected model.
        log.info("Start pulling the '{}' model ... would take several minutes ...", modelName);
        Container.ExecResult r = ollama.execInContainer("ollama", "pull", modelName);
        log.info("Model pulling competed! {}", r);
        ollama.commitToImage(dockerImageNameTC);

        return ollama;
    }
}
