package com.mediconnect.notification;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface NotificationLogRepository extends MongoRepository<NotificationLog, String> {

    List<NotificationLog> findAllByOrderByAtDesc(Pageable pageable);
}
