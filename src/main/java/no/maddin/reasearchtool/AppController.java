package no.maddin.reasearchtool;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AppController {

    private final Assistant assistant;

    @GetMapping(path = "/chat/{prompt}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ChatResponse> chat(@PathVariable("prompt") String prompt) {
        String response = assistant.chat(prompt);
        return ResponseEntity.ok(ChatResponse.builder().response(response).build());
    }
}
