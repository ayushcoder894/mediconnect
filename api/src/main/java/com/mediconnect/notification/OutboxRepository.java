package com.mediconnect.notification;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxMessage, Long> {

    boolean existsByIdempotencyKey(String key);

    @Query("""
            select m.id from OutboxMessage m
            where m.status = com.mediconnect.notification.OutboxMessage.Status.PENDING
              and m.nextAttemptAt <= :now
            order by m.nextAttemptAt""")
    List<Long> findDueIds(LocalDateTime now, Pageable pageable);

    /**
     * Atomically claims a message. Only one worker (even across instances) gets a
     * row count of 1, so a message is never sent twice concurrently.
     */
    @Modifying
    @Query("""
            update OutboxMessage m set m.status = com.mediconnect.notification.OutboxMessage.Status.SENDING,
                                       m.nextAttemptAt = :now
            where m.id = :id and m.status = com.mediconnect.notification.OutboxMessage.Status.PENDING""")
    int claim(Long id, LocalDateTime now);

    /** Returns rows stuck in SENDING (e.g. the instance crashed mid-send) to the queue. */
    @Modifying
    @Query("""
            update OutboxMessage m set m.status = com.mediconnect.notification.OutboxMessage.Status.PENDING
            where m.status = com.mediconnect.notification.OutboxMessage.Status.SENDING
              and m.nextAttemptAt < :before""")
    int releaseStuck(LocalDateTime before);

    List<OutboxMessage> findAllByOrderByIdDesc(Pageable pageable);

    long countByStatus(OutboxMessage.Status status);
}
