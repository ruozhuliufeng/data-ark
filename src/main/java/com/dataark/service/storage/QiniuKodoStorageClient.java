package com.dataark.service.storage;

import com.dataark.model.StorageConfig;
import com.qiniu.common.QiniuException;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import org.apache.commons.lang3.StringUtils;

import java.io.File;

public class QiniuKodoStorageClient extends AbstractStorageClient {
    private final Auth auth;
    private final UploadManager uploadManager;
    private final BucketManager bucketManager;

    public QiniuKodoStorageClient(StorageConfig storage) {
        super(storage);
        requireAccessKeys();
        Configuration configuration = new Configuration(region(storage));
        configuration.resumableUploadMaxConcurrentTaskCount = Math.max(1, storage.getUploadConcurrency() == null ? 4 : storage.getUploadConcurrency());
        configuration.retryMax = Math.max(1, storage.getUploadRetries() == null ? 3 : storage.getUploadRetries());
        this.auth = Auth.create(storage.getAccessKey(), storage.getSecretKey());
        this.uploadManager = new UploadManager(configuration);
        this.bucketManager = new BucketManager(auth, configuration);
    }

    @Override
    public void put(String objectKey, File file) {
        try {
            uploadManager.put(file, objectKey, auth.uploadToken(storage.getBucket(), objectKey));
        } catch (QiniuException e) {
            throw new IllegalStateException("Qiniu Kodo upload failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean exists(String objectKey) {
        try {
            bucketManager.stat(storage.getBucket(), objectKey);
            return true;
        } catch (QiniuException e) {
            return false;
        }
    }

    @Override
    public void checkRoot(String prefix) {
        try {
            bucketManager.listFiles(storage.getBucket(), StringUtils.defaultString(prefix), null, 1, null);
        } catch (QiniuException e) {
            throw new IllegalStateException("Qiniu Kodo bucket check failed: " + e.getMessage(), e);
        }
    }

    private Region region(StorageConfig storage) {
        String region = StringUtils.trimToEmpty(storage.getRegion());
        if (StringUtils.isBlank(region)) {
            return Region.autoRegion();
        }
        if (StringUtils.equalsAnyIgnoreCase(region, "z0", "huadong", "cn-east-1")) {
            return Region.huadong();
        }
        if (StringUtils.equalsAnyIgnoreCase(region, "cn-east-2", "huadong-zhejiang-2")) {
            return Region.huadongZheJiang2();
        }
        if (StringUtils.equalsAnyIgnoreCase(region, "z1", "huabei", "cn-north-1")) {
            return Region.huabei();
        }
        if (StringUtils.equalsAnyIgnoreCase(region, "z2", "huanan", "cn-south-1")) {
            return Region.huanan();
        }
        if (StringUtils.equalsAnyIgnoreCase(region, "na0", "beimei", "us-north-1")) {
            return Region.beimei();
        }
        if (StringUtils.equalsAnyIgnoreCase(region, "as0", "xinjiapo", "ap-southeast-1")) {
            return Region.xinjiapo();
        }
        return Region.createWithRegionId(region);
    }
}
