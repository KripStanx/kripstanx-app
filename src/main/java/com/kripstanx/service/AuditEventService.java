package com.kripstanx.service;

import com.kripstanx.config.audit.AuditEventConverter;
import com.kripstanx.domain.PersistentAuditEvent;
import com.kripstanx.repository.PersistenceAuditEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing audit events.
 * <p>
 * This is the default implementation to support SpringBoot Actuator AuditEventRepository
 */
@Service
@Transactional
public class AuditEventService {
    private final Logger log = LoggerFactory.getLogger(AuditEventService.class);

    private final PersistenceAuditEventRepository persistenceAuditEventRepository;

    private final AuditEventConverter auditEventConverter;

    private final ArchiveAndPurgeLogService archiveAndPurgeLogService;

    public AuditEventService(
        PersistenceAuditEventRepository persistenceAuditEventRepository,
        AuditEventConverter auditEventConverter, ArchiveAndPurgeLogService archiveAndPurgeLogService) {

        this.persistenceAuditEventRepository = persistenceAuditEventRepository;
        this.auditEventConverter = auditEventConverter;
        this.archiveAndPurgeLogService = archiveAndPurgeLogService;
    }

    public Page<AuditEvent> findAll(Pageable pageable) {
        return persistenceAuditEventRepository.findAll(pageable)
                                              .map(auditEventConverter::convertToAuditEvent);
    }

    public Page<AuditEvent> findByDates(Instant fromDate, Instant toDate, Pageable pageable) {
        return persistenceAuditEventRepository.findAllByAuditEventDateBetween(fromDate, toDate, pageable)
                                              .map(auditEventConverter::convertToAuditEvent);
    }

    public Optional<AuditEvent> find(Long id) {
        return Optional.ofNullable(persistenceAuditEventRepository.findById(id))
                       .filter(Optional::isPresent)
                       .map(Optional::get)
                       .map(auditEventConverter::convertToAuditEvent);
    }

    public List<AuditEvent> findTop3ByPrincipalOrderByAuditEventDateDesc(String principal) {
        Iterable<PersistentAuditEvent> persistentAuditEvents = persistenceAuditEventRepository.findTop3ByPrincipalIgnoreCaseOrderByAuditEventDateDesc(
            principal);
        return auditEventConverter.convertToAuditEvent(persistentAuditEvents);
    }

    public List<AuditEvent> getAuditEventsByUserOrderByAuditEventDateDesc(String userName) {
        List<PersistentAuditEvent> persistentAuditEvents = persistenceAuditEventRepository.findByPrincipalIgnoreCaseOrderByAuditEventDateDesc(
            userName);
        return persistentAuditEvents.stream()
                                    .map(auditEventConverter::convertToAuditEvent)
                                    .collect(Collectors.toList());
    }

    public List<AuditEvent> getSuccesfulAuditEventsByUserOrderByAuditEventDateDesc(String userName) {
        List<PersistentAuditEvent> persistentAuditEvents = persistenceAuditEventRepository.findTop2ByPrincipalIgnoreCaseAndAuditEventTypeOrderByAuditEventDateDesc(
            userName,
            "AUTHENTICATION_SUCCESS");
        return persistentAuditEvents.stream()
                                    .map(auditEventConverter::convertToAuditEvent)
                                    .collect(Collectors.toList());
    }

    public void purge(int olderThanInDays) {
        log.info("Purge olderThanInDays {}", olderThanInDays);
        long originalTableCount = persistenceAuditEventRepository.count();
        long originalTableSize = archiveAndPurgeLogService.getTableSize("jhi_persistent_audit_event");

        List<PersistentAuditEvent> oldRows = persistenceAuditEventRepository
            .findByAuditEventDateBefore(ArchiveAndPurgeLogService.getOlderThanInstant(olderThanInDays));

        persistenceAuditEventRepository.deleteAll(oldRows);

        long currentTableCount = persistenceAuditEventRepository.count();
        long currentTableSize = archiveAndPurgeLogService.getTableSize("jhi_persistent_audit_event");
        archiveAndPurgeLogService.save("Purge_AuditEvent",
                                       originalTableCount - currentTableCount,
                                       originalTableSize - currentTableSize);
    }
}
