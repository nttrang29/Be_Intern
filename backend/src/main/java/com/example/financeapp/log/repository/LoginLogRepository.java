package com.example.financeapp.log.repository;

import com.example.financeapp.log.entity.LoginLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoginLogRepository extends JpaRepository<LoginLog, Long> {

    /**
     * Lấy lịch sử đăng nhập của 1 user (mới nhất trước)
     */
    List<LoginLog> findByUserIdOrderByLoginTimeDesc(Long userId);

    /**
     * Lấy N bản ghi mới nhất
     */
    List<LoginLog> findTop10ByUserIdOrderByLoginTimeDesc(Long userId);
}

