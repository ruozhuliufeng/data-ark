package com.dataark.service.storage;

import com.dataark.model.StorageConfig;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class StorageObjectKeyService {
    public String objectKey(StorageConfig storage, String fileName) {
        String basePath = normalizePath(storage.getBasePath());
        if (StringUtils.isBlank(basePath)) {
            return fileName;
        }
        return basePath + "/" + fileName;
    }

    public String probeKey(StorageConfig storage, String fileName) {
        String basePath = normalizePath(storage.getBasePath());
        if (StringUtils.isBlank(basePath)) {
            return "_probe/" + fileName;
        }
        return basePath + "/_probe/" + fileName;
    }

    public String displayPath(StorageConfig storage, String objectKey) {
        if (StringUtils.isNotBlank(storage.getDomain())) {
            return trimRightSlash(storage.getDomain()) + "/" + objectKey;
        }
        if (storage.getType() == null) {
            return storage.getBucket() + "/" + objectKey;
        }
        return storage.getType().name().toLowerCase() + "://" + storage.getBucket() + "/" + objectKey;
    }

    public String normalizePath(String value) {
        return trimSlashes(StringUtils.defaultIfBlank(value, "/"));
    }

    private String trimSlashes(String value) {
        String path = StringUtils.defaultString(value).trim();
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    private String trimRightSlash(String value) {
        String path = StringUtils.defaultString(value).trim();
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }
}
