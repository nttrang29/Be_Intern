
package com.example.financeapp.repository;

import com.example.financeapp.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    List<Wallet> findByUser_UserId(Long userId);
    boolean existsByWalletNameAndUser_UserId(String walletName, Long userId);
}