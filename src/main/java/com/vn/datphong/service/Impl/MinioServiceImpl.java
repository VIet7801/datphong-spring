package com.vn.datphong.service.Impl;

import com.vn.datphong.config.MinioProperties;
import com.vn.datphong.service.MinioService;
import io.minio.*;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class MinioServiceImpl implements MinioService {

    private static final Logger log = LoggerFactory.getLogger(MinioServiceImpl.class);

    private final MinioClient minioClient;
    private final MinioProperties properties;

    public MinioServiceImpl(MinioClient minioClient, MinioProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    @Override
    public String getBucket() {
        return properties.getBucket();
    }

    @Override
    public void createBucket(String bucketName) throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(bucketName)
                        .build()
        );

        if (!exists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(bucketName)
                            .build()
            );
            log.info("MinIO bucket created: {}", bucketName);
        }
    }

    @Override
    public void upload(
            String bucketName,
            String objectName,
            InputStream inputStream,
            long size,
            String contentType
    ) throws Exception {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(inputStream, size, -1)
                        .contentType(contentType)
                        .build()
        );
        log.info("Upload success: {}/{}", bucketName, objectName);
    }

    @Override
    public void removeObject(String bucketName, String objectName) throws Exception {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );
        log.info("Remove object success: {}/{}", bucketName, objectName);
    }

    @Override
    public String getObjectUrl(String bucketName, String objectName) {
        String endpoint = properties.getEndpoint().replaceAll("/+$", "");
        return endpoint + "/" + bucketName + "/" + objectName;
    }

    @Override
    public String getPresignedUrl(String bucketName, String objectName) throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucketName)
                        .object(objectName)
                        .expiry(1, TimeUnit.HOURS)
                        .build()
        );
    }

    @Override
    public void setPublic(String bucketName) throws Exception {
        String policy = """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": "*",
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/*"]
                    }
                  ]
                }
                """.formatted(bucketName);

        minioClient.setBucketPolicy(
                SetBucketPolicyArgs.builder()
                        .bucket(bucketName)
                        .config(policy)
                        .build()
        );
        log.info("MinIO bucket set to PUBLIC: {}", bucketName);
    }

    @Override
    public String uploadExcel(byte[] data, String folder, String fileName) throws Exception {
        String bucketName = getBucket();
        createBucket(bucketName);

        String objectName = folder + "/" + UUID.randomUUID() + "_" + fileName;

        try (InputStream inputStream = new ByteArrayInputStream(data)) {
            upload(
                    bucketName,
                    objectName,
                    inputStream,
                    data.length,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            );
        }

        log.info("Upload Excel success: {}/{}", bucketName, objectName);
        return getObjectUrl(bucketName, objectName);
    }
}