package com.kripstanx.domain;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "archive_and_purge_log")
public class ArchiveAndPurgeLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @Column(name = "task_name")
    private String taskName;

    @Column(name = "processed_row_count")
    private Long processedRowCount;

    @Column(name = "reduction_mb")
    private Long reductionMb;

    @Column(name = "operation_time")
    private Instant operationTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public ArchiveAndPurgeLog taskName(String taskName) {
        this.taskName = taskName;
        return this;
    }

    public Long getProcessedRowCount() {
        return processedRowCount;
    }

    public void setProcessedRowCount(Long processedRowCount) {
        this.processedRowCount = processedRowCount;
    }

    public ArchiveAndPurgeLog processedRowCount(Long processedRowCount) {
        this.processedRowCount = processedRowCount;
        return this;
    }

    public Long getReductionMb() {
        return reductionMb;
    }

    public void setReductionMb(Long reductionMb) {
        this.reductionMb = reductionMb;
    }

    public ArchiveAndPurgeLog reductionMb(Long reductionMb) {
        this.reductionMb = reductionMb;
        return this;
    }

    public Instant getOperationTime() {
        return operationTime;
    }

    public void setOperationTime(Instant operationTime) {
        this.operationTime = operationTime;
    }

    public ArchiveAndPurgeLog operationTime(Instant operationTime) {
        this.operationTime = operationTime;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ArchiveAndPurgeLog archiveAndPurgeLog = (ArchiveAndPurgeLog) o;
        if (archiveAndPurgeLog.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), archiveAndPurgeLog.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ArchiveAndPurgeLog{");
        sb.append("id=").append(id);
        sb.append(", taskName='").append(taskName).append('\'');
        sb.append(", processedRowCount=").append(processedRowCount);
        sb.append(", reductionMb=").append(reductionMb);
        sb.append(", operationTime=").append(operationTime);
        sb.append('}');
        return sb.toString();
    }
}
