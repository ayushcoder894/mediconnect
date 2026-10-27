package com.mediconnect.assistant;

import com.mediconnect.common.ApiException;
import com.mediconnect.risk.AiClient;
import com.mediconnect.security.CurrentUser;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssistantService {

    /** How many earlier messages are sent to the model as conversation context. */
    private static final int HISTORY_WINDOW = 10;

    private final ChatSessionRepository sessions;
    private final AiClient ai;

    public record ChatRequest(String sessionId, @NotBlank @Size(max = 1000) String message) {
    }

    public record SessionSummary(String id, String title, Instant updatedAt) {
    }

    public ChatSession chat(ChatRequest req) {
        Long userId = CurrentUser.get().id();
        ChatSession session = req.sessionId() == null
                ? newSession(userId, req.message())
                : sessions.findByIdAndUserId(req.sessionId(), userId)
                .orElseThrow(() -> ApiException.notFound("Conversation"));

        List<ChatSession.Message> all = session.getMessages();
        List<AiClient.ChatTurn> history = all.subList(Math.max(0, all.size() - HISTORY_WINDOW), all.size()).stream()
                .map(m -> new AiClient.ChatTurn(m.role(), m.content()))
                .toList();

        AiClient.ChatReply reply = ai.chat(new AiClient.ChatRequest(req.message().trim(), history));

        Instant now = Instant.now();
        all.add(new ChatSession.Message("user", req.message().trim(), List.of(), null, null, now));
        all.add(new ChatSession.Message("assistant", reply.reply(), reply.sources(),
                reply.suggestedSpecialization(), reply.mode(), Instant.now()));
        session.setUpdatedAt(now);
        return sessions.save(session);
    }

    public List<SessionSummary> mySessions() {
        return sessions.findSummariesByUserId(CurrentUser.get().id()).stream()
                .map(s -> new SessionSummary(s.getId(), s.getTitle(), s.getUpdatedAt()))
                .toList();
    }

    public ChatSession get(String id) {
        return sessions.findByIdAndUserId(id, CurrentUser.get().id())
                .orElseThrow(() -> ApiException.notFound("Conversation"));
    }

    public void delete(String id) {
        sessions.delete(get(id));
    }

    private ChatSession newSession(Long userId, String firstMessage) {
        ChatSession s = new ChatSession();
        s.setUserId(userId);
        String title = firstMessage.trim();
        s.setTitle(title.length() > 60 ? title.substring(0, 57) + "..." : title);
        return s;
    }
}
