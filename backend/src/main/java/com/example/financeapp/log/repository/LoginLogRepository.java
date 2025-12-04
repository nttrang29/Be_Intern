package com.example.financeapp.log.repository;

import com.example.financeapp.log.entity.LoginLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoginLogRepository extends JpaRepository<LoginLog, Long>, JpaSpecificationExecutor<LoginLog> {

    /**
     * Lấy lịch sử đăng nhập của 1 user (mới nhất trước)
     */
    List<LoginLog> findByUserIdOrderByLoginTimeDesc(Long userId);

    /**
     * Lấy N bản ghi mới nhất
     */
    List<LoginLog> findTop10ByUserIdOrderByLoginTimeDesc(Long userId);

    Page<LoginLog> findByUserId(Long userId, Pageable pageable);

    Optional<LoginLog> findTopByUserIdOrderByLoginTimeDesc(Long userId);
}

