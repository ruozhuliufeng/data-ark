package com.dataark.service.storage;

import com.dataark.model.StorageConfig;
import com.obs.services.ObsClient;
import com.obs.services.ObsConfiguration;
import com.obs.services.model.AccessControlList;
import com.obs.services.model.ListObjectsRequest;
import com.obs.services.model.PutObjectRequest;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.InputStream;

public class HuaweiObsStorageClient extends AbstractStorageClient {
    private final ObsClient client;

    public HuaweiObsStorageClient(StorageConfig storage) {
        super(storage);
        requireAccessKeys();
        ObsConfiguration config = new ObsConfiguration();
        config.setEndPoint(endpoint(storage));
        config.setPathStyle(Boolean.TRUE.equals(storage.getPathStyleAccess()));
        config.setMaxConnections(Math.max(1, storage.getUploadConcurrency() == null ? 4 : storage.getUploadConcurrency()));
        config.setMaxErrorRetry(Math.max(1, storage.getUploadRetries() == null ? 3 : storage.getUploadRetries()));
        this.client = new ObsClient(storage.getAccessKey(), storage.getSecretKey(), config);
    }

    @Override
    public void put(String objectKey, File file) {
        PutObjectRequest request = new PutObjectRequest(storage.getBucket(), objectKey, file);
        applyAcl(request);
        client.putObject(request);
    }

    @Override
    public void put(String objectKey, InputStream input, long contentLength) {
        PutObjectRequest request = new PutObjectRequest(storage.getBucket(), objectKey, input);
        applyAcl(request);
        client.putObject(request);
    }

    private void applyAcl(PutObjectRequest request) {
        AccessControlList acl = acl(storage.getAcl());
        if (acl != null) {
            request.setAcl(acl);
        }
    }

    @Override
    public boolean exists(String objectKey) {
        return client.doesObjectExist(storage.getBucket(), objectKey);
    }

    @Override
    public void checkRoot(String prefix) {
        ListObjectsRequest request = new ListObjectsRequest(storage.getBucket());
        request.setPrefix(StringUtils.defaultString(prefix));
        request.setMaxKeys(1);
        client.listObjects(request);
    }

    private String endpoint(StorageConfig storage) {
        if (StringUtils.isNotBlank(storage.getEndpoint())) {
            return storage.getEndpoint();
        }
        if (StringUtils.isBlank(storage.getRegion())) {
            throw new IllegalArgumentException("Endpoint or region is required for Huawei OBS");
        }
        return "https://obs." + storage.getRegion() + ".myhuaweicloud.com";
    }

    private AccessControlList acl(String acl) {
        if (StringUtils.equalsIgnoreCase(acl, "public-read")) {
            return AccessControlList.REST_CANNED_PUBLIC_READ;
        }
        if (StringUtils.equalsIgnoreCase(acl, "public-read-write")) {
            return AccessControlList.REST_CANNED_PUBLIC_READ_WRITE;
        }
        if (StringUtils.equalsIgnoreCase(acl, "private")) {
            return AccessControlList.REST_CANNED_PRIVATE;
        }
        return null;
    }
}
