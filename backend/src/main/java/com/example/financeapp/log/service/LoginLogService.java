package com.example.financeapp.log.service;

import com.example.financeapp.log.entity.LoginLog;
import com.example.financeapp.log.repository.LoginLogRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class LoginLogService {

    private final LoginLogRepository loginLogRepository;

    public LoginLogService(LoginLogRepository loginLogRepository) {
        this.loginLogRepository = loginLogRepository;
    }

    /**
     * Lưu 1 bản ghi login log mới
     */
    public void save(Long userId, String ipAddress, String userAgent) {
        LoginLog log = new LoginLog();
        log.setUserId(userId);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        log.setLoginTime(Instant.now());

        loginLogRepository.save(log);
    }

    /**
     * Lấy toàn bộ lịch sử đăng nhập của 1 user (mới -> cũ)
     */
    public List<LoginLog> getLogsByUser(Long userId) {
        return loginLogRepository.findByUserIdOrderByLoginTimeDesc(userId);
    }

    /**
     * Lấy N lần đăng nhập gần nhất của user
     */
    public List<LoginLog> getRecentLogsByUser(Long userId, int limit) {
        return loginLogRepository.findTop10ByUserIdOrderByLoginTimeDesc(userId);
    }
}

