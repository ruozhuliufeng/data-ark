package com.dataark.repository;

import com.dataark.model.StorageConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StorageConfigRepository extends JpaRepository<StorageConfig, Long> {
}
