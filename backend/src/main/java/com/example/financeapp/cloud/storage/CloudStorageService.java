package com.example.financeapp.cloud.storage;

/**
 * Service trừu tượng để upload file lên cloud storage
 */
public interface CloudStorageService {

    /**
     * Upload file và trả về URL truy cập
     *
     * @param key         Đường dẫn file trong bucket
     * @param data        Nội dung file dạng byte
     * @param contentType Content-Type của file
     * @return URL công khai (hoặc signed URL) của file
     */
    String uploadFile(String key, byte[] data, String contentType);

    /**
     * Kiểm tra tính năng backup có đang bật không
     */
    boolean isEnabled();
}

