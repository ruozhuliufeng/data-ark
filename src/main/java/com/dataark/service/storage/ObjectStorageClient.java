package com.dataark.service.storage;

import java.io.File;
import java.io.InputStream;

public interface ObjectStorageClient {
    void put(String objectKey, File file);

    void put(String objectKey, InputStream input, long contentLength);

    boolean exists(String objectKey);

    void checkRoot(String prefix);
}
