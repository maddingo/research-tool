package no.maddin.reasearchtool;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ChatResponse {
    String response;
    String source;
}
