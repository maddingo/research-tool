package no.maddin.reasearchtool;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

@Configuration
@Profile("dev")
@RequiredArgsConstructor
public class DevBeansConfig implements BeanConfig {

    private final Environment environment;

    @Bean
    @Override
    public EmbeddingStore<TextSegment> embeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }

    @Bean
    @Override
    public ChatLanguageModel chatModel() {
        return OpenAiChatModel.builder()
                .apiKey(environment.getProperty("researchtool.openai_api_key"))
                .modelName(GPT_4_O_MINI)
                .build();

    }
}
