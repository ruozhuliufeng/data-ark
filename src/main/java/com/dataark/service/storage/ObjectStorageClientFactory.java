package com.dataark.service.storage;

import com.dataark.model.StorageConfig;
import com.dataark.model.StorageType;
import org.springframework.stereotype.Service;

@Service
public class ObjectStorageClientFactory {
    public ObjectStorageClient create(StorageConfig storage) {
        if (Boolean.FALSE.equals(storage.getEnabled())) {
            throw new IllegalStateException("Storage platform is disabled: " + storage.getName());
        }
        if (storage.getType() == StorageType.ALIYUN_OSS) {
            return new AliyunOssStorageClient(storage);
        }
        if (storage.getType() == StorageType.MINIO) {
            return new MinioStorageClient(storage);
        }
        if (storage.getType() == StorageType.TENCENT_COS) {
            return new TencentCosStorageClient(storage);
        }
        if (storage.getType() == StorageType.QINIU) {
            return new QiniuKodoStorageClient(storage);
        }
        if (storage.getType() == StorageType.HUAWEI_OBS) {
            return new HuaweiObsStorageClient(storage);
        }
        if (storage.getType() == StorageType.WEBDAV) {
            return new WebdavStorageClient(storage);
        }
        if (storage.getType() == StorageType.S3) {
            return new AwsS3StorageClient(storage);
        }
        throw new IllegalArgumentException("Unsupported storage type: " + storage.getType());
    }
}
