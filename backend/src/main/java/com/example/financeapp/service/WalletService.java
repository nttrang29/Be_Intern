
package com.example.financeapp.service;

import com.example.financeapp.dto.CreateWalletRequest;
import com.example.financeapp.entity.Wallet;

import java.util.List;

public interface WalletService {
    Wallet createWallet(Long userId, CreateWalletRequest request);
    List<Wallet> getWalletsByUserId(Long userId);
}