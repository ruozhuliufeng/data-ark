package com.dataark.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "storage_config")
public class StorageConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StorageType type;

    private String rcloneRemote;

    @NotBlank
    @Column(nullable = false)
    private String bucket;

    private String accessKey;
    private String secretKey;
    private String region;
    private String endpoint;
    private String vendor;
    private String webdavUrl;
    private String webdavUsername;
    private String webdavPassword;
    private Long multipartThresholdMb = 100L;
    private Long multipartChunkMb = 64L;
    private Integer uploadRetries = 3;

    @Column(nullable = false)
    private String basePath = "/";

    @Column(length = 4000)
    private String configJson;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public StorageType getType() {
        return type;
    }

    public void setType(StorageType type) {
        this.type = type;
    }

    public String getRcloneRemote() {
        return rcloneRemote;
    }

    public void setRcloneRemote(String rcloneRemote) {
        this.rcloneRemote = rcloneRemote;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getWebdavUrl() {
        return webdavUrl;
    }

    public void setWebdavUrl(String webdavUrl) {
        this.webdavUrl = webdavUrl;
    }

    public String getWebdavUsername() {
        return webdavUsername;
    }

    public void setWebdavUsername(String webdavUsername) {
        this.webdavUsername = webdavUsername;
    }

    public String getWebdavPassword() {
        return webdavPassword;
    }

    public void setWebdavPassword(String webdavPassword) {
        this.webdavPassword = webdavPassword;
    }

    public Long getMultipartThresholdMb() {
        return multipartThresholdMb;
    }

    public void setMultipartThresholdMb(Long multipartThresholdMb) {
        this.multipartThresholdMb = multipartThresholdMb;
    }

    public Long getMultipartChunkMb() {
        return multipartChunkMb;
    }

    public void setMultipartChunkMb(Long multipartChunkMb) {
        this.multipartChunkMb = multipartChunkMb;
    }

    public Integer getUploadRetries() {
        return uploadRetries;
    }

    public void setUploadRetries(Integer uploadRetries) {
        this.uploadRetries = uploadRetries;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getConfigJson() {
        return configJson;
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
