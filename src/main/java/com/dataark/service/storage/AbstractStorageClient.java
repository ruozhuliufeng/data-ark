package com.dataark.service.storage;

import com.dataark.model.StorageConfig;
import org.apache.commons.lang3.StringUtils;

abstract class AbstractStorageClient implements ObjectStorageClient {
    protected final StorageConfig storage;

    AbstractStorageClient(StorageConfig storage) {
        this.storage = storage;
    }

    @Override
    public void checkRoot(String prefix) {
        if (StringUtils.isBlank(storage.getBucket())) {
            throw new IllegalArgumentException("Bucket is required");
        }
        if (StringUtils.isBlank(prefix)) {
            return;
        }
        exists(prefix.endsWith("/") ? prefix : prefix + "/");
    }

    protected void requireAccessKeys() {
        if (StringUtils.isBlank(storage.getAccessKey()) || StringUtils.isBlank(storage.getSecretKey())) {
            throw new IllegalArgumentException("AK/SK is required for " + storage.getType());
        }
    }
}
