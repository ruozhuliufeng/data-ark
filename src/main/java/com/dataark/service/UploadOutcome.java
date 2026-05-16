package com.dataark.service;

public class UploadOutcome {
    private String remotePath;
    private boolean multipartUpload;
    private int totalParts;
    private int uploadedParts;
    private String manifestFile;

    public String getRemotePath() {
        return remotePath;
    }

    public void setRemotePath(String remotePath) {
        this.remotePath = remotePath;
    }

    public boolean isMultipartUpload() {
        return multipartUpload;
    }

    public void setMultipartUpload(boolean multipartUpload) {
        this.multipartUpload = multipartUpload;
    }

    public int getTotalParts() {
        return totalParts;
    }

    public void setTotalParts(int totalParts) {
        this.totalParts = totalParts;
    }

    public int getUploadedParts() {
        return uploadedParts;
    }

    public void setUploadedParts(int uploadedParts) {
        this.uploadedParts = uploadedParts;
    }

    public String getManifestFile() {
        return manifestFile;
    }

    public void setManifestFile(String manifestFile) {
        this.manifestFile = manifestFile;
    }
}
