package com.mediconnect.assistant;

import com.mediconnect.risk.AiClient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * One MediBot conversation. Messages are embedded in the session document: they
 * are always read together and vary in shape (sources, suggestions), which suits
 * MongoDB better than a relational table.
 */
@Document("chat_sessions")
@Getter
@Setter
@NoArgsConstructor
public class ChatSession {

    @Id
    private String id;

    @Indexed
    private Long userId;

    private String title;

    private List<Message> messages = new ArrayList<>();

    private Instant createdAt = Instant.now();

    @Indexed
    private Instant updatedAt = Instant.now();

    public record Message(String role, String content, List<AiClient.Source> sources,
                          String suggestedSpecialization, String mode, Instant at) {
    }
}
