package com.dataark.service.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ListObjectsRequest;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.dataark.model.StorageConfig;
import org.apache.commons.lang3.StringUtils;

import java.io.File;

public class AliyunOssStorageClient extends AbstractStorageClient {
    private final OSS client;

    public AliyunOssStorageClient(StorageConfig storage) {
        super(storage);
        requireAccessKeys();
        this.client = new OSSClientBuilder().build(endpoint(storage), storage.getAccessKey(), storage.getSecretKey());
    }

    @Override
    public void put(String objectKey, File file) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.length());
        if (StringUtils.isNotBlank(storage.getAcl())) {
            metadata.setHeader("x-oss-object-acl", storage.getAcl());
        }
        client.putObject(new PutObjectRequest(storage.getBucket(), objectKey, file, metadata));
    }

    @Override
    public boolean exists(String objectKey) {
        return client.doesObjectExist(storage.getBucket(), objectKey);
    }

    @Override
    public void checkRoot(String prefix) {
        client.listObjects(new ListObjectsRequest(storage.getBucket()).withPrefix(StringUtils.defaultString(prefix)).withMaxKeys(1));
    }

    private String endpoint(StorageConfig storage) {
        if (StringUtils.isNotBlank(storage.getEndpoint())) {
            return storage.getEndpoint();
        }
        if (StringUtils.isBlank(storage.getRegion())) {
            throw new IllegalArgumentException("Endpoint or region is required for Aliyun OSS");
        }
        return "https://oss-" + storage.getRegion() + ".aliyuncs.com";
    }
}
