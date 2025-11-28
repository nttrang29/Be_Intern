package com.example.financeapp.cloud.storage.impl;

import com.example.financeapp.cloud.storage.CloudStorageProperties;
import com.example.financeapp.cloud.storage.CloudStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Triển khai CloudStorageService bằng AWS S3
 */
@Service
public class S3CloudStorageService implements CloudStorageService {

    private static final Logger log = LoggerFactory.getLogger(S3CloudStorageService.class);

    private final CloudStorageProperties properties;
    private final S3Client s3Client;

    public S3CloudStorageService(CloudStorageProperties properties) {
        this.properties = properties;

        if (!properties.isEnabled()) {
            log.warn("Cloud backup is disabled. Set cloud.aws.enabled=true to enable S3 backup.");
            this.s3Client = null;
            return;
        }

        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(properties.getRegion()));

        if (StringUtils.hasText(properties.getAccessKey()) && StringUtils.hasText(properties.getSecretKey())) {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(
                    properties.getAccessKey(), properties.getSecretKey());
            builder = builder.credentialsProvider(StaticCredentialsProvider.create(credentials));
        } else {
            builder = builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        this.s3Client = builder.build();
    }

    @Override
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    @Override
    public String uploadFile(String key, byte[] data, String contentType) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("Cloud backup is disabled. Please enable cloud.aws.enabled.");
        }
        if (s3Client == null) {
            throw new IllegalStateException("S3 client chưa được khởi tạo.");
        }

        String normalizedKey = buildKey(key);
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(normalizedKey)
                    .contentType(contentType)
                    .contentLength((long) data.length)
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(data));

            return s3Client.utilities()
                    .getUrl(GetUrlRequest.builder()
                            .bucket(properties.getBucketName())
                            .key(normalizedKey)
                            .build())
                    .toExternalForm();
        } catch (SdkClientException ex) {
            log.error("Lỗi khi upload file lên S3: {}", ex.getMessage(), ex);
            throw new RuntimeException("Upload backup thất bại: " + ex.getMessage(), ex);
        }
    }

    private String buildKey(String key) {
        String basePath = properties.getBasePath();
        if (!StringUtils.hasText(basePath)) {
            basePath = "backups";
        }
        if (basePath.endsWith("/")) {
            basePath = basePath.substring(0, basePath.length() - 1);
        }
        if (key.startsWith("/")) {
            key = key.substring(1);
        }
        return basePath + "/" + key;
    }
}

