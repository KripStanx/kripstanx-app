package com.kripstanx.repository;

import com.kripstanx.domain.ArchiveAndPurgeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ArchiveAndPurgeLogRepository extends JpaRepository<ArchiveAndPurgeLog, Long> {

    List<ArchiveAndPurgeLog> findAllByOperationTimeBefore(Instant olderThan);

    @Query(value = "select segment_name table_name, bytes/1024/1024 mb "
        + "from dba_segments where segment_name = :tableName",
           nativeQuery = true)
    List<Object[]> getTableRowCountAndSize(@Param("tableName") String tableName);
}
