package com.mediconnect.assistant;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;

    @PostMapping("/chat")
    public ChatSession chat(@Valid @RequestBody AssistantService.ChatRequest req) {
        return assistantService.chat(req);
    }

    @GetMapping("/sessions")
    public List<AssistantService.SessionSummary> sessions() {
        return assistantService.mySessions();
    }

    @GetMapping("/sessions/{id}")
    public ChatSession session(@PathVariable String id) {
        return assistantService.get(id);
    }

    @DeleteMapping("/sessions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        assistantService.delete(id);
    }
}
