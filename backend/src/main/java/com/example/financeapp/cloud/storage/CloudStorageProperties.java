package com.example.financeapp.cloud.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình cho cloud storage (AWS S3)
 */
@ConfigurationProperties(prefix = "cloud.aws")
public class CloudStorageProperties {

    /**
     * Bật/tắt tính năng backup cloud
     */
    private boolean enabled = false;

    /**
     * Khu vực của bucket (ví dụ ap-southeast-1)
     */
    private String region;

    /**
     * Tên bucket để lưu trữ backup
     */
    private String bucketName;

    /**
     * Access key và secret key (nếu không set sẽ dùng default credential chain)
     */
    private String accessKey;
    private String secretKey;

    /**
     * Thư mục gốc trong bucket (ví dụ backups/)
     */
    private String basePath = "backups";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }
}

