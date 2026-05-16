package com.dataark.service;

import com.dataark.config.DataArkProperties;
import com.dataark.model.StorageConfig;
import com.dataark.service.storage.ObjectStorageClient;
import com.dataark.service.storage.ObjectStorageClientFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Service
public class ResumableUploadService {
    private static final long MB = 1024L * 1024L;

    private final DataArkProperties properties;
    private final ObjectStorageClientFactory clientFactory;

    public ResumableUploadService(DataArkProperties properties,
                                  ObjectStorageClientFactory clientFactory) {
        this.properties = properties;
        this.clientFactory = clientFactory;
    }

    public UploadOutcome upload(File file,
                                StorageConfig storage,
                                String remotePath,
                                StringBuilder commandLog) throws IOException {
        if (file.length() < thresholdBytes(storage)) {
            return uploadSingle(file, storage, remotePath, commandLog);
        }
        return uploadMultipart(file, storage, remotePath, null, commandLog);
    }

    public UploadOutcome resume(File file,
                                StorageConfig storage,
                                String remotePath,
                                String manifestFile,
                                StringBuilder commandLog) throws IOException {
        return uploadMultipart(file, storage, remotePath, manifestFile, commandLog);
    }

    private UploadOutcome uploadSingle(File file,
                                       StorageConfig storage,
                                       String remotePath,
                                       StringBuilder commandLog) throws IOException {
        ObjectStorageClient client = clientFactory.create(storage);
        commandLog.append("SDK upload ").append(file.getAbsolutePath()).append(" -> ")
                .append(remotePath).append(System.lineSeparator());
        client.put(remotePath, file);
        UploadOutcome outcome = new UploadOutcome();
        outcome.setRemotePath(remotePath);
        outcome.setMultipartUpload(false);
        outcome.setTotalParts(1);
        outcome.setUploadedParts(1);
        return outcome;
    }

    private UploadOutcome uploadMultipart(File file,
                                          StorageConfig storage,
                                          String remotePath,
                                          String manifestFile,
                                          StringBuilder commandLog) throws IOException {
        MultipartManifest manifest = loadOrCreateManifest(file, storage, remotePath, manifestFile);

        ObjectStorageClient client = clientFactory.create(storage);
        int uploaded = countUploaded(manifest);
        for (int i = 0; i < manifest.totalParts; i++) {
            String partRemote = partRemotePath(remotePath, file.getName(), i);
            if (remoteExists(client, partRemote, commandLog)) {
                markUploaded(manifest, i);
                uploaded = countUploaded(manifest);
                saveManifest(manifest);
                continue;
            }
            commandLog.append("SDK multipart upload part ").append(i + 1).append("/")
                    .append(manifest.totalParts).append(" offset=")
                    .append(partOffset(manifest, i)).append(", bytes=")
                    .append(partLength(file, manifest, i)).append(" -> ")
                    .append(partRemote).append(System.lineSeparator());
            try {
                uploadPart(client, partRemote, file, manifest, i);
            } catch (Exception e) {
                saveManifest(manifest);
                throw new IllegalStateException("Multipart upload failed at part " + (i + 1) + "/" + manifest.totalParts
                        + ", uploadedParts=" + uploaded + ", message=" + e.getMessage(), e);
            }
            markUploaded(manifest, i);
            uploaded = countUploaded(manifest);
            saveManifest(manifest);
        }

        UploadOutcome outcome = new UploadOutcome();
        outcome.setRemotePath(remotePath);
        outcome.setMultipartUpload(true);
        outcome.setTotalParts(manifest.totalParts);
        outcome.setUploadedParts(uploaded);
        outcome.setManifestFile(manifest.manifestFile);
        return outcome;
    }

    private MultipartManifest loadOrCreateManifest(File file,
                                                   StorageConfig storage,
                                                   String remotePath,
                                                   String manifestFile) throws IOException {
        if (StringUtils.isNotBlank(manifestFile)) {
            return readManifest(new File(manifestFile));
        }
        MultipartManifest manifest = new MultipartManifest();
        manifest.sourceFile = file.getAbsolutePath();
        manifest.remotePath = remotePath;
        manifest.chunkBytes = chunkBytes(storage);
        manifest.totalParts = (int) ((file.length() + manifest.chunkBytes - 1) / manifest.chunkBytes);
        File manifestDir = new File(properties.getWorkDir(), "multipart-manifests");
        manifestDir.mkdirs();
        manifest.manifestFile = new File(manifestDir, safeName(file.getName()) + "-" + System.currentTimeMillis() + ".properties").getAbsolutePath();
        manifest.uploadedParts = "";
        saveManifest(manifest);
        return manifest;
    }

    private MultipartManifest readManifest(File file) throws IOException {
        Properties props = new Properties();
        FileInputStream input = new FileInputStream(file);
        try {
            props.load(input);
        } finally {
            input.close();
        }
        MultipartManifest manifest = new MultipartManifest();
        manifest.manifestFile = file.getAbsolutePath();
        manifest.sourceFile = props.getProperty("sourceFile");
        manifest.remotePath = props.getProperty("remotePath");
        manifest.chunkBytes = Long.parseLong(props.getProperty("chunkBytes"));
        manifest.totalParts = Integer.parseInt(props.getProperty("totalParts"));
        manifest.uploadedParts = props.getProperty("uploadedParts", "");
        return manifest;
    }

    private void saveManifest(MultipartManifest manifest) throws IOException {
        Properties props = new Properties();
        props.setProperty("sourceFile", manifest.sourceFile);
        props.setProperty("remotePath", manifest.remotePath);
        props.setProperty("chunkBytes", String.valueOf(manifest.chunkBytes));
        props.setProperty("totalParts", String.valueOf(manifest.totalParts));
        props.setProperty("uploadedParts", manifest.uploadedParts);
        FileWriter writer = new FileWriter(manifest.manifestFile);
        try {
            props.store(writer, "DataArk multipart upload manifest");
        } finally {
            writer.close();
        }
    }

    private void uploadPart(ObjectStorageClient client,
                            String partRemote,
                            File source,
                            MultipartManifest manifest,
                            int part) throws IOException {
        FileInputStream fileInput = new FileInputStream(source);
        try {
            skipFully(fileInput, partOffset(manifest, part));
            InputStream limited = new LimitedInputStream(new BufferedInputStream(fileInput, 1024 * 1024), partLength(source, manifest, part));
            client.put(partRemote, limited, partLength(source, manifest, part));
        } finally {
            fileInput.close();
        }
    }

    private long partOffset(MultipartManifest manifest, int part) {
        return manifest.chunkBytes * part;
    }

    private long partLength(File source, MultipartManifest manifest, int part) {
        long offset = partOffset(manifest, part);
        return Math.min(manifest.chunkBytes, source.length() - offset);
    }

    private void skipFully(InputStream input, long bytes) throws IOException {
        long remaining = bytes;
        while (remaining > 0) {
            long skipped = input.skip(remaining);
            if (skipped <= 0) {
                if (input.read() == -1) {
                    throw new IOException("Unexpected end of file while skipping " + bytes + " bytes");
                }
                skipped = 1;
            }
            remaining -= skipped;
        }
    }

    private boolean remoteExists(ObjectStorageClient client, String remotePath, StringBuilder commandLog) {
        if (client.exists(remotePath)) {
            commandLog.append("Part exists, skip: ").append(remotePath).append(System.lineSeparator());
            return true;
        }
        return false;
    }

    private int countUploaded(MultipartManifest manifest) {
        return StringUtils.isBlank(manifest.uploadedParts) ? 0 : manifest.uploadedParts.split(",").length;
    }

    private void markUploaded(MultipartManifest manifest, int part) {
        String token = String.valueOf(part);
        if (StringUtils.equals(manifest.uploadedParts, token)
                || StringUtils.startsWith(manifest.uploadedParts, token + ",")
                || StringUtils.endsWith(manifest.uploadedParts, "," + token)
                || StringUtils.contains(manifest.uploadedParts, "," + token + ",")) {
            return;
        }
        if (StringUtils.isBlank(manifest.uploadedParts)) {
            manifest.uploadedParts = token;
        } else {
            manifest.uploadedParts = manifest.uploadedParts + "," + token;
        }
    }

    private String partRemotePath(String remotePath, String fileName, int part) {
        return remotePath + ".parts/" + safeName(fileName) + ".part" + String.format("%05d", part);
    }

    private long thresholdBytes(StorageConfig storage) {
        return Math.max(1L, defaultLong(storage.getMultipartThresholdMb(), 100L)) * MB;
    }

    private long chunkBytes(StorageConfig storage) {
        return chunkMb(storage) * MB;
    }

    private long chunkMb(StorageConfig storage) {
        return Math.max(5L, defaultLong(storage.getMultipartChunkMb(), 64L));
    }

    private long defaultLong(Long value, long fallback) {
        return value == null ? fallback : value;
    }

    private String safeName(String value) {
        return StringUtils.defaultString(value, "file").replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static class MultipartManifest {
        private String manifestFile;
        private String sourceFile;
        private String remotePath;
        private long chunkBytes;
        private int totalParts;
        private String uploadedParts;
    }

    private static class LimitedInputStream extends InputStream {
        private final InputStream delegate;
        private long remaining;

        LimitedInputStream(InputStream delegate, long limit) {
            this.delegate = delegate;
            this.remaining = limit;
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int value = delegate.read();
            if (value != -1) {
                remaining--;
            }
            return value;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int read = delegate.read(b, off, (int) Math.min(len, remaining));
            if (read > 0) {
                remaining -= read;
            }
            return read;
        }
    }
}
