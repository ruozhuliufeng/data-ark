package com.dataark.repository;

import com.dataark.model.ExecutionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExecutionRecordRepository extends JpaRepository<ExecutionRecord, Long> {
    List<ExecutionRecord> findTop100ByOrderByStartedAtDesc();

    List<ExecutionRecord> findTop50ByJobIdOrderByStartedAtDesc(Long jobId);
}
