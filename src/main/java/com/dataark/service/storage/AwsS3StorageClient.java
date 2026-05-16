package com.dataark.service.storage;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.dataark.model.StorageConfig;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.InputStream;

public class AwsS3StorageClient extends AbstractStorageClient {
    private final AmazonS3 client;

    public AwsS3StorageClient(StorageConfig storage) {
        super(storage);
        requireAccessKeys();
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(storage.getAccessKey(), storage.getSecretKey())))
                .withClientConfiguration(clientConfiguration(storage));
        if (StringUtils.isNotBlank(storage.getEndpoint())) {
            builder.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(storage.getEndpoint(), region(storage)));
        } else {
            builder.withRegion(region(storage));
        }
        if (Boolean.TRUE.equals(storage.getPathStyleAccess())) {
            builder.withPathStyleAccessEnabled(true);
        }
        this.client = builder.build();
    }

    @Override
    public void put(String objectKey, File file) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.length());
        PutObjectRequest request = new PutObjectRequest(storage.getBucket(), objectKey, file).withMetadata(metadata);
        applyAcl(request);
        client.putObject(request);
    }

    @Override
    public void put(String objectKey, InputStream input, long contentLength) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(contentLength);
        PutObjectRequest request = new PutObjectRequest(storage.getBucket(), objectKey, input, metadata);
        applyAcl(request);
        client.putObject(request);
    }

    private void applyAcl(PutObjectRequest request) {
        CannedAccessControlList acl = acl(storage.getAcl());
        if (acl != null) {
            request.withCannedAcl(acl);
        }
    }

    @Override
    public boolean exists(String objectKey) {
        return client.doesObjectExist(storage.getBucket(), objectKey);
    }

    @Override
    public void checkRoot(String prefix) {
        if (StringUtils.isBlank(prefix)) {
            client.listObjects(new ListObjectsRequest().withBucketName(storage.getBucket()).withMaxKeys(1));
        } else {
            client.listObjects(new ListObjectsRequest().withBucketName(storage.getBucket()).withPrefix(prefix).withMaxKeys(1));
        }
    }

    private ClientConfiguration clientConfiguration(StorageConfig storage) {
        ClientConfiguration config = new ClientConfiguration();
        config.setMaxConnections(Math.max(1, storage.getUploadConcurrency() == null ? 4 : storage.getUploadConcurrency()));
        config.setMaxErrorRetry(Math.max(1, storage.getUploadRetries() == null ? 3 : storage.getUploadRetries()));
        return config;
    }

    private String region(StorageConfig storage) {
        return StringUtils.defaultIfBlank(storage.getRegion(), "us-east-1");
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
