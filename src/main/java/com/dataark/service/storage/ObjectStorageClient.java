package com.dataark.service.storage;

import java.io.File;

public interface ObjectStorageClient {
    void put(String objectKey, File file);

    boolean exists(String objectKey);

    void checkRoot(String prefix);
}
