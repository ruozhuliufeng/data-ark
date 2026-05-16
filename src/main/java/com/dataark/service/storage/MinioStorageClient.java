package com.dataark.service.storage;

import com.dataark.model.StorageConfig;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;

public class MinioStorageClient extends AbstractStorageClient {
    private final MinioClient client;

    public MinioStorageClient(StorageConfig storage) {
        super(storage);
        requireAccessKeys();
        if (StringUtils.isBlank(storage.getEndpoint())) {
            throw new IllegalArgumentException("Endpoint is required for MinIO");
        }
        this.client = MinioClient.builder()
                .endpoint(storage.getEndpoint())
                .credentials(storage.getAccessKey(), storage.getSecretKey())
                .build();
    }

    @Override
    public void put(String objectKey, File file) {
        try {
            FileInputStream input = new FileInputStream(file);
            try {
                client.putObject(PutObjectArgs.builder()
                        .bucket(storage.getBucket())
                        .object(objectKey)
                        .stream(input, file.length(), -1)
                        .build());
            } finally {
                input.close();
            }
        } catch (Exception e) {
            throw new IllegalStateException("MinIO upload failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean exists(String objectKey) {
        try {
            client.statObject(StatObjectArgs.builder().bucket(storage.getBucket()).object(objectKey).build());
            return true;
        } catch (ErrorResponseException e) {
            return false;
        } catch (Exception e) {
            throw new IllegalStateException("MinIO stat failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void checkRoot(String prefix) {
        try {
            if (!client.bucketExists(BucketExistsArgs.builder().bucket(storage.getBucket()).build())) {
                throw new IllegalArgumentException("Bucket does not exist: " + storage.getBucket());
            }
        } catch (Exception e) {
            throw new IllegalStateException("MinIO bucket check failed: " + e.getMessage(), e);
        }
    }
}
