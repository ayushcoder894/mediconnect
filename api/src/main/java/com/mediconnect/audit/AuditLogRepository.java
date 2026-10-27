package com.mediconnect.audit;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AuditLogRepository extends MongoRepository<AuditLog, String> {

    List<AuditLog> findAllByOrderByAtDesc(Pageable pageable);

    List<AuditLog> findByActionOrderByAtDesc(String action, Pageable pageable);
}
