package com.kripstanx.repository;

import com.kripstanx.domain.PersistentAuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.List;

/**
 * Spring Data JPA repository for the PersistentAuditEvent entity.
 */
public interface PersistenceAuditEventRepository extends JpaRepository<PersistentAuditEvent, Long>,
    JpaSpecificationExecutor<PersistentAuditEvent> {

    List<PersistentAuditEvent> findTop2ByPrincipalIgnoreCaseAndAuditEventTypeOrderByAuditEventDateDesc(
        String principal,
        String eventType
    );

    List<PersistentAuditEvent> findByPrincipalIgnoreCaseOrderByAuditEventDateDesc(String principal);

    List<PersistentAuditEvent> findTop3ByPrincipalIgnoreCaseOrderByAuditEventDateDesc(String principal);

    List<PersistentAuditEvent> findByAuditEventDateAfter(Instant after);

    List<PersistentAuditEvent> findByAuditEventDateBefore(Instant before);

    List<PersistentAuditEvent> findByPrincipalAndAuditEventDateAfter(String principal, Instant after);

    List<PersistentAuditEvent> findByPrincipalIgnoreCaseAndAuditEventDateAfterAndAuditEventType(String principle,
                                                                                                Instant after,
                                                                                                String type);

    Page<PersistentAuditEvent> findAllByAuditEventDateBetween(Instant fromDate, Instant toDate, Pageable pageable);
}
