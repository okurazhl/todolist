package com.smartmemo.memo.infrastructure.storage;

import io.minio.*;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 文件存储适配器。
 * 封装 MinIO 操作，业务层不直接依赖 MinIO SDK。
 */
@Component
public class MinioStorageAdapter {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageAdapter.class);

    private final MinioClient client;
    private final String bucket;

    public MinioStorageAdapter(MinioConfigProperties props) {
        this.client = MinioClient.builder()
                .endpoint(props.getEndpoint())
                .credentials(props.getAccessKey(), props.getSecretKey())
                .build();
        this.bucket = props.getBucket();
        ensureBucket();
    }

    /**
     * 上传文件到 MinIO。
     * @return object key
     */
    public String upload(UUID userId, UUID memoId, String fileName, InputStream data, long size, String contentType) {
        String objectKey = userId + "/" + memoId + "/" + UUID.randomUUID() + "-" + fileName;
        try {
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(data, size, -1)
                    .contentType(contentType)
                    .build());
            log.info("File uploaded: bucket={}, object={}, size={}", bucket, objectKey, size);
            return objectKey;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file to MinIO", e);
        }
    }

    /**
     * 删除文件。
     */
    public void delete(String objectKey) {
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectKey).build());
            log.info("File deleted: bucket={}, object={}", bucket, objectKey);
        } catch (Exception e) {
            log.warn("Failed to delete file: bucket={}, object={}", bucket, objectKey, e);
        }
    }

    /**
     * 生成预签名下载 URL（1小时有效）。
     */
    public String presignedDownloadUrl(String objectKey) {
        try {
            return client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .method(Method.GET)
                    .expiry(1, TimeUnit.HOURS)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate presigned URL", e);
        }
    }

    private void ensureBucket() {
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Bucket created: {}", bucket);
            }
        } catch (Exception e) {
            log.warn("Failed to ensure bucket exists: {}", bucket, e);
        }
    }
}
