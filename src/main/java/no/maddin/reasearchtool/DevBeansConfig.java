package no.maddin.reasearchtool;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Image;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Configuration
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevBeansConfig implements BeanConfig {

    static final String OLLAMA_IMAGE = "ollama/ollama:latest";
    static final String TINY_DOLPHIN_MODEL = "tinydolphin";
    static final String DOCKER_IMAGE_NAME = "tc-ollama/ollama:latest-tinydolphin";

//    private final Environment environment;
    private final ApplicationContext applicationContext;
    private final AtomicReference<OllamaContainer> container = new AtomicReference<>();

    @Bean
    @Override
    public EmbeddingStore<TextSegment> embeddingStore() {
        return new InMemoryEmbeddingStore<>();
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

    private OllamaContainer createOllamaContainer() {
//        String ollamaImage = environment.getProperty("researchtool.ollama.image", OLLAMA_IMAGE);
//        String modelName = environment.getProperty("researchtool.ollama.model", TINY_DOLPHIN_MODEL);

        DockerImageName dockerImageName = DockerImageName.parse(OLLAMA_IMAGE);
        DockerClient dockerClient = DockerClientFactory.instance().client();
        List<Image> images = dockerClient.listImagesCmd().withReferenceFilter(DOCKER_IMAGE_NAME).exec();
        OllamaContainer ollama;
        if (images.isEmpty()) {
            ollama = new OllamaContainer(dockerImageName);
        } else {
            ollama = new OllamaContainer(DockerImageName.parse(DOCKER_IMAGE_NAME).asCompatibleSubstituteFor(OLLAMA_IMAGE));
        }
        ollama.start();

        // Pull the model and create an image based on the selected model.
        try {

            log.info("Start pulling the '{}' model ... would take several minutes ...", TINY_DOLPHIN_MODEL);
            Container.ExecResult r = ollama.execInContainer("ollama", "pull", TINY_DOLPHIN_MODEL);
            log.info("Model pulling competed! {}", r);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error pulling model", e);
        }
        ollama.commitToImage(DOCKER_IMAGE_NAME);

        return ollama;
    }

    @Bean
    @Override
    public ChatLanguageModel chatModel() {
//        String modelName = environment.getProperty("researchtool.ollama.model", TINY_DOLPHIN_MODEL);
        OllamaContainer ollama = applicationContext.getBean(OllamaContainer.class);
        return OllamaChatModel.builder()
            .baseUrl(ollama.getEndpoint())
            .temperature(0.0)
            .logRequests(true)
            .logResponses(true)
            .modelName(TINY_DOLPHIN_MODEL)
            .build();
    }

    @PreDestroy
    public void stopContainer() {
        container.getAndUpdate(c -> {
            if (c != null) {
                c.stop();
            }
            return null;
        });
    }
}
