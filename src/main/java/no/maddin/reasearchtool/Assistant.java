package no.maddin.reasearchtool;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;

@AiService
interface Assistant {

    @SystemMessage("You are a friendly assistant that answers in the same language as the question.")
    String chat(String userMessage);
}

