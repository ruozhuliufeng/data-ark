package com.dataark.service.storage;

import com.dataark.model.StorageConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.CannedAccessControlList;
import com.qcloud.cos.model.ListObjectsRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import org.apache.commons.lang3.StringUtils;

import java.io.File;

public class TencentCosStorageClient extends AbstractStorageClient {
    private final COSClient client;

    public TencentCosStorageClient(StorageConfig storage) {
        super(storage);
        requireAccessKeys();
        COSCredentials credentials = new BasicCOSCredentials(storage.getAccessKey(), storage.getSecretKey());
        ClientConfig config = new ClientConfig(new Region(region(storage)));
        config.setMaxConnectionsCount(Math.max(1, storage.getUploadConcurrency() == null ? 4 : storage.getUploadConcurrency()));
        config.setMaxErrorRetry(Math.max(1, storage.getUploadRetries() == null ? 3 : storage.getUploadRetries()));
        this.client = new COSClient(credentials, config);
    }

    @Override
    public void put(String objectKey, File file) {
        PutObjectRequest request = new PutObjectRequest(storage.getBucket(), objectKey, file);
        CannedAccessControlList acl = acl(storage.getAcl());
        if (acl != null) {
            request.withCannedAcl(acl);
        }
        client.putObject(request);
    }

    @Override
    public boolean exists(String objectKey) {
        return client.doesObjectExist(storage.getBucket(), objectKey);
    }

    @Override
    public void checkRoot(String prefix) {
        ListObjectsRequest request = new ListObjectsRequest();
        request.setBucketName(storage.getBucket());
        request.setPrefix(StringUtils.defaultString(prefix));
        request.setMaxKeys(1);
        client.listObjects(request);
    }

    private String region(StorageConfig storage) {
        if (StringUtils.isBlank(storage.getRegion())) {
            throw new IllegalArgumentException("Region is required for Tencent COS, for example ap-guangzhou");
        }
        return storage.getRegion();
    }

    private CannedAccessControlList acl(String acl) {
        if (StringUtils.equalsIgnoreCase(acl, "public-read")) {
            return CannedAccessControlList.PublicRead;
        }
        if (StringUtils.equalsIgnoreCase(acl, "public-read-write")) {
            return CannedAccessControlList.PublicReadWrite;
        }
        if (StringUtils.equalsIgnoreCase(acl, "private")) {
            return CannedAccessControlList.Private;
        }
        return null;
    }
}
