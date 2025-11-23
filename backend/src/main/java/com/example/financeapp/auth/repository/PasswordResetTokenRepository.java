package com.example.financeapp.auth.repository;

import com.example.financeapp.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    @Query("SELECT prt FROM PasswordResetToken prt WHERE prt.user.email = :email AND prt.status = :status")
    Optional<PasswordResetToken> findByUserEmailAndStatus(@Param("email") String email, @Param("status") PasswordResetToken.Status status);
}

