package com.mediconnect.assistant;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ChatSessionRepository extends MongoRepository<ChatSession, String> {

    @Query(value = "{ 'userId': ?0 }", fields = "{ 'messages': 0 }", sort = "{ 'updatedAt': -1 }")
    List<ChatSession> findSummariesByUserId(Long userId);

    Optional<ChatSession> findByIdAndUserId(String id, Long userId);

    long count();
}
