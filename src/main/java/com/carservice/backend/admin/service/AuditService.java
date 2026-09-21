package com.carservice.backend.admin.service;

import com.carservice.backend.admin.dto.AdminAuditLogDetailResponse;
import com.carservice.backend.admin.dto.AdminAuditLogResponse;
import com.carservice.backend.admin.dto.AdminAuditSummaryResponse;
import com.carservice.backend.admin.entity.AuditLog;
import com.carservice.backend.admin.repository.AuditLogRepository;
import com.carservice.backend.common.exception.ResourceNotFoundException;
import com.carservice.backend.user.entity.User;
import tools.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private static final Set<String> SENSITIVE_PATTERNS = Set.of(
            "password", "token", "refreshtoken", "accesstoken", "secret", "secretkey",
            "authorization", "credential", "creditcard", "cvv", "apikey", "privatekey",
            "razorpaysecret", "razorpaykeysecret"
    );

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Record an audit event using implicit security and request contexts.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog record(
            String action,
            String entityType,
            String entityId,
            String description,
            String status,
            Object beforeState,
            Object afterState,
            Map<String, Object> metadata
    ) {
        return record(null, action, entityType, entityId, description, status, beforeState, afterState, metadata);
    }

    /**
     * Record an audit event with explicit actor override.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog record(
            User explicitActor,
            String action,
            String entityType,
            String entityId,
            String description,
            String status,
            Object beforeState,
            Object afterState,
            Map<String, Object> metadata
    ) {
        try {
            AuditLog entry = new AuditLog();
            entry.setCreatedAt(LocalDateTime.now());
            entry.setAction(action != null ? action.trim().toUpperCase() : "UNKNOWN_ACTION");
            entry.setEntityType(entityType != null ? entityType.trim().toUpperCase() : "SYSTEM");
            entry.setEntityId(entityId != null ? entityId.trim() : null);
            entry.setDescription(description);
            entry.setStatus(status != null ? status.trim().toUpperCase() : "SUCCESS");

            // Resolve Actor
            if (explicitActor != null) {
                entry.setActorUserId(explicitActor.getId());
                entry.setActorEmail(explicitActor.getEmail());
                entry.setActorName(explicitActor.getName());
                entry.setActorRole(explicitActor.getRole() != null ? explicitActor.getRole().name() : "USER");
            } else {
                resolveActorFromSecurityContext(entry);
            }

            // Resolve IP & User-Agent
            resolveRequestDetails(entry);

            // Redact and serialize before/after states
            Object redactedBefore = redactSensitive(beforeState);
            Object redactedAfter = redactSensitive(afterState);

            if (redactedBefore != null) {
                entry.setBeforeStateJson(objectMapper.writeValueAsString(redactedBefore));
            }
            if (redactedAfter != null) {
                entry.setAfterStateJson(objectMapper.writeValueAsString(redactedAfter));
            }

            // Generate Diff if both before & after are present
            if (redactedBefore != null && redactedAfter != null) {
                Map<String, Object> diffMap = new LinkedHashMap<>();
                diffMap.put("before", redactedBefore);
                diffMap.put("after", redactedAfter);
                entry.setDiffJson(objectMapper.writeValueAsString(diffMap));
            }

            // Metadata serialization
            if (metadata != null && !metadata.isEmpty()) {
                Object redactedMetadata = redactSensitive(metadata);
                if (redactedMetadata != null) {
                    entry.setMetadataJson(objectMapper.writeValueAsString(redactedMetadata));
                }
            }

            AuditLog saved = auditLogRepository.save(entry);
            log.debug("AuditLog recorded: id={}, action={}, entityType={}, entityId={}, status={}",
                    saved.getId(), saved.getAction(), saved.getEntityType(), saved.getEntityId(), saved.getStatus());
            return saved;
        } catch (Exception e) {
            log.error("Failed to record audit event: action={}, entityType={}, entityId={}",
                    action, entityType, entityId, e);
            return null;
        }
    }

    /**
     * Search audit logs with dynamic multi-attribute specifications.
     */
    @Transactional(readOnly = true)
    public Page<AdminAuditLogResponse> searchAuditLogs(
            String action,
            String entityType,
            String entityId,
            String status,
            Long actorUserId,
            String actorEmail,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String search,
            Pageable pageable
    ) {
        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (action != null && !action.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("action")), action.trim().toUpperCase()));
            }
            if (entityType != null && !entityType.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("entityType")), entityType.trim().toUpperCase()));
            }
            if (entityId != null && !entityId.isBlank()) {
                predicates.add(cb.equal(root.get("entityId"), entityId.trim()));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase()));
            }
            if (actorUserId != null) {
                predicates.add(cb.equal(root.get("actorUserId"), actorUserId));
            }
            if (actorEmail != null && !actorEmail.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("actorEmail")), "%" + actorEmail.trim().toLowerCase() + "%"));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate searchPredicate = cb.or(
                        cb.like(cb.lower(root.get("action")), pattern),
                        cb.like(cb.lower(root.get("entityType")), pattern),
                        cb.like(cb.lower(root.get("entityId")), pattern),
                        cb.like(cb.lower(root.get("actorEmail")), pattern),
                        cb.like(cb.lower(root.get("actorName")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                );
                predicates.add(searchPredicate);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return auditLogRepository.findAll(spec, pageable).map(AdminAuditLogResponse::new);
    }

    /**
     * Get single audit log detail with full diff and JSON states.
     */
    @Transactional(readOnly = true)
    public AdminAuditLogDetailResponse getAuditLogDetail(Long id) {
        AuditLog logEntry = auditLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Audit log entry not found with id: " + id));
        return new AdminAuditLogDetailResponse(logEntry);
    }

    /**
     * Get aggregate KPI summary of audit records.
     */
    @Transactional(readOnly = true)
    public AdminAuditSummaryResponse getAuditSummary() {
        long totalEvents = auditLogRepository.count();
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        long todayEvents = auditLogRepository.countByCreatedAtAfter(startOfToday);
        long failedEvents = auditLogRepository.countByStatus("FAILED");
        long securityEvents = auditLogRepository.countByAction("SECURITY_EVENT") + auditLogRepository.countByAction("LOGIN_FAILED");

        List<AdminAuditLogResponse> recentEvents = auditLogRepository.findTop5ByOrderByCreatedAtDesc().stream()
                .map(AdminAuditLogResponse::new)
                .toList();

        return new AdminAuditSummaryResponse(totalEvents, todayEvents, failedEvents, securityEvents, recentEvents);
    }

    private void resolveActorFromSecurityContext(AuditLog entry) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth.getPrincipal() instanceof String && "anonymousUser".equals(auth.getPrincipal()))) {
            if (auth.getPrincipal() instanceof User user) {
                entry.setActorUserId(user.getId());
                entry.setActorEmail(user.getEmail());
                entry.setActorName(user.getName());
                entry.setActorRole(user.getRole() != null ? user.getRole().name() : "USER");
                return;
            }
            entry.setActorEmail(auth.getName());
            entry.setActorName(auth.getName());
            if (auth.getAuthorities() != null && !auth.getAuthorities().isEmpty()) {
                entry.setActorRole(auth.getAuthorities().iterator().next().getAuthority());
            } else {
                entry.setActorRole("AUTHENTICATED");
            }
        } else {
            entry.setActorUserId(null);
            entry.setActorEmail("SYSTEM");
            entry.setActorName("System Engine");
            entry.setActorRole("SYSTEM");
        }
    }

    private void resolveRequestDetails(AuditLog entry) {
        try {
            RequestAttributes reqAttrs = RequestContextHolder.getRequestAttributes();
            if (reqAttrs instanceof ServletRequestAttributes servletAttrs) {
                HttpServletRequest request = servletAttrs.getRequest();
                String forwarded = request.getHeader("X-Forwarded-For");
                if (forwarded != null && !forwarded.isBlank()) {
                    entry.setIpAddress(forwarded.split(",")[0].trim());
                } else {
                    entry.setIpAddress(request.getRemoteAddr());
                }
                String ua = request.getHeader("User-Agent");
                if (ua != null) {
                    entry.setUserAgent(ua.length() > 250 ? ua.substring(0, 250) : ua);
                }
            }
        } catch (Exception e) {
            log.debug("Could not resolve servlet request details for audit log: {}", e.getMessage());
        }

        if (entry.getIpAddress() == null || entry.getIpAddress().isBlank()) {
            entry.setIpAddress("127.0.0.1");
        }
    }

    @SuppressWarnings("unchecked")
    private Object redactSensitive(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Map<?, ?> map) {
            Map<String, Object> redacted = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                String normalized = key.toLowerCase().replace("_", "").replace("-", "");
                if (normalized.equals("key") || SENSITIVE_PATTERNS.stream().anyMatch(normalized::contains)) {
                    redacted.put(key, "[REDACTED]");
                } else {
                    redacted.put(key, redactSensitive(entry.getValue()));
                }
            }
            return redacted;
        } else if (obj instanceof List<?> list) {
            List<Object> redactedList = new ArrayList<>();
            for (Object item : list) {
                redactedList.add(redactSensitive(item));
            }
            return redactedList;
        } else if (obj instanceof String || obj instanceof Number || obj instanceof Boolean) {
            return obj;
        } else {
            try {
                Map<String, Object> map = objectMapper.convertValue(obj, Map.class);
                return redactSensitive(map);
            } catch (Exception e) {
                return obj.toString();
            }
        }
    }
}
