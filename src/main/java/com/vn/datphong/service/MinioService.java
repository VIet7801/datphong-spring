package com.vn.datphong.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface MinioService {
    /**
     * Lấy tên bucket mặc định từ cấu hình.
     */
    String getBucket();

    /**
     * Tạo bucket trên MinIO nếu chưa tồn tại.
     */
    void createBucket(String bucketName) throws Exception;

    /**
     * Tải lên một tệp (stream) lên MinIO.
     */
    void upload(
            String bucketName,
            String objectName,
            InputStream inputStream,
            long size,
            String contentType
    ) throws Exception;

    /**
     * Xóa một object khỏi bucket trên MinIO.
     */
    void removeObject(
            String bucketName,
            String objectName
    ) throws Exception;

    /**
     * Lấy URL truy cập trực tiếp đến object.
     */
    String getObjectUrl(
            String bucketName,
            String objectName
    );

    /**
     * Lấy URL tạm thời (presigned URL) có thời hạn.
     */
    String getPresignedUrl(
            String bucketName,
            String objectName
    ) throws Exception;

    /**
     * Thiết lập quyền đọc công khai cho bucket.
     */
    void setPublic(String bucketName) throws Exception;

    /**
     * Tải lên tệp Excel và trả về đường dẫn truy cập.
     */
    String uploadExcel(
            byte[] data,
            String folder,
            String fileName
    ) throws Exception;
}


