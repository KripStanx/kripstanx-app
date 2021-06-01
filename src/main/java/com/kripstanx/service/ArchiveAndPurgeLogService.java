package com.kripstanx.service;

import com.kripstanx.domain.ArchiveAndPurgeLog;
import com.kripstanx.repository.ArchiveAndPurgeLogRepository;
import org.hibernate.dialect.H2Dialect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class ArchiveAndPurgeLogService {

    private final Logger log = LoggerFactory.getLogger(ArchiveAndPurgeLogService.class);

    @Value("${spring.jpa.database-platform}")
    private String databasePlatform;

    private final ArchiveAndPurgeLogRepository archiveAndPurgeLogRepository;

    public ArchiveAndPurgeLogService(ArchiveAndPurgeLogRepository archiveAndPurgeLogRepository) {
        this.archiveAndPurgeLogRepository = archiveAndPurgeLogRepository;
    }

    /**
     * Save a ArchiveAndPurgeLog.
     *
     * @return the persisted entity
     */
    public ArchiveAndPurgeLog save(String taskName, Long processedRowCount, Long reductionMb) {
        ArchiveAndPurgeLog archiveAndPurgeLog = new ArchiveAndPurgeLog().taskName(taskName)
                                                                        .processedRowCount(processedRowCount)
                                                                        .reductionMb(reductionMb)
                                                                        .operationTime(Instant.now());
        log.debug("Request to save ArchiveAndPurgeLog : {}", archiveAndPurgeLog);
        archiveAndPurgeLog = archiveAndPurgeLogRepository.save(archiveAndPurgeLog);
        return archiveAndPurgeLog;
    }

    public void purge(int olderThanInDays) {
        log.info("Purge olderThanInDays {}", olderThanInDays);

        List<ArchiveAndPurgeLog> deletable = archiveAndPurgeLogRepository.findAllByOperationTimeBefore(
            ArchiveAndPurgeLogService.getOlderThanInstant(olderThanInDays));
        long originalTableSize = getTableSize("ARCHIVE_AND_PURGE_LOG");
        long deletedSize = deletable.size();

        archiveAndPurgeLogRepository.deleteAll(deletable);
        long currentTableSize = getTableSize("ARCHIVE_AND_PURGE_LOG");
        save("Purge_ArchiveAndPurgeLog",
             deletedSize,
             originalTableSize - currentTableSize);
    }

    public long getTableSize(String tableName) {
        try {
            if (Objects.equals(databasePlatform, H2Dialect.class.getName())) {
                return 0;
            } else {
                List<Object[]> tableSize = archiveAndPurgeLogRepository.getTableRowCountAndSize(tableName);
                return ((Number) tableSize.get(0)[1]).longValue();
            }
        } catch (Exception e) {
            return 0;
        }
    }

    public static Instant getOlderThanInstant(int olderThanInDays) {
        return LocalDateTime.now()
                            .toLocalDate()
                            .atStartOfDay()
                            .toInstant(ZoneOffset.UTC)
                            .minus(olderThanInDays - 1, ChronoUnit.DAYS);
    }
}
