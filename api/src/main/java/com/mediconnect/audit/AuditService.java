package com.mediconnect.audit;

import com.mediconnect.security.AuthUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repository;

    /**
     * Records an action by the current user. Auditing must never break the business
     * operation, so failures are logged and swallowed.
     */
    public void record(String action, String entityType, Long entityId, String details) {
        try {
            AuditLog entry = new AuditLog();
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof AuthUser user) {
                entry.setActorId(user.id());
                entry.setActorEmail(user.email());
                entry.setActorRole(user.role().name());
            } else {
                entry.setActorRole("SYSTEM");
            }
            entry.setAction(action);
            entry.setEntityType(entityType);
            entry.setEntityId(entityId);
            entry.setDetails(details);
            repository.save(entry);
        } catch (RuntimeException e) {
            log.warn("Could not write audit log for {}: {}", action, e.getMessage());
        }
    }

    public List<AuditLog> recent(String action, int limit) {
        var page = PageRequest.of(0, Math.min(limit, 500));
        return action == null || action.isBlank()
                ? repository.findAllByOrderByAtDesc(page)
                : repository.findByActionOrderByAtDesc(action, page);
    }
}
