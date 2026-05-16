package com.dataark.service.storage;

import com.dataark.model.StorageConfig;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import org.apache.commons.lang3.StringUtils;

import java.io.File;

public class WebdavStorageClient extends AbstractStorageClient {
    private final Sardine sardine;
    private final String rootUrl;

    public WebdavStorageClient(StorageConfig storage) {
        super(storage);
        if (StringUtils.isBlank(storage.getWebdavUrl())) {
            throw new IllegalArgumentException("WebDAV address is required");
        }
        this.sardine = SardineFactory.begin(
                StringUtils.defaultString(storage.getWebdavUsername()),
                StringUtils.defaultString(storage.getWebdavPassword()));
        this.rootUrl = trimRightSlash(storage.getWebdavUrl()) + "/" + trimSlashes(storage.getBucket());
    }

    @Override
    public void put(String objectKey, File file) {
        try {
            ensureParentDirectories(objectKey);
            sardine.put(url(objectKey), file, null, true);
        } catch (Exception e) {
            throw new IllegalStateException("WebDAV upload failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean exists(String objectKey) {
        try {
            return sardine.exists(url(objectKey));
        } catch (Exception e) {
            throw new IllegalStateException("WebDAV exists check failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void checkRoot(String prefix) {
        try {
            if (!sardine.exists(rootUrl)) {
                throw new IllegalArgumentException("WebDAV root does not exist: " + rootUrl);
            }
        } catch (Exception e) {
            throw new IllegalStateException("WebDAV root check failed: " + e.getMessage(), e);
        }
    }

    private void ensureParentDirectories(String objectKey) throws Exception {
        String[] parts = trimSlashes(objectKey).split("/");
        String current = rootUrl;
        for (int i = 0; i < parts.length - 1; i++) {
            current = current + "/" + parts[i];
            if (!sardine.exists(current)) {
                sardine.createDirectory(current);
            }
        }
    }

    private String url(String objectKey) {
        return rootUrl + "/" + trimSlashes(objectKey);
    }

    private String trimRightSlash(String value) {
        String path = StringUtils.defaultString(value).trim();
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
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
}
