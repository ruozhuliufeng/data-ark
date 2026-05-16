package com.dataark.repository;

import com.dataark.model.BackupJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BackupJobRepository extends JpaRepository<BackupJob, Long> {
    List<BackupJob> findByStatus(com.dataark.model.JobStatus status);
}
