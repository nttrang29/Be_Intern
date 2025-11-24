package com.example.financeapp.auth.repository;

import com.example.financeapp.auth.model.OtpPurpose;
import com.example.financeapp.auth.model.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    void deleteByEmailAndPurpose(String email, OtpPurpose purpose);

    Optional<OtpToken> findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(
            String email,
            OtpPurpose purpose
    );
}

