package com.example.financeapp.service;

import com.example.financeapp.dto.CreateTransactionRequest;
import com.example.financeapp.entity.Transaction;

import java.util.List;

public interface TransactionService {
    Transaction createExpense(Long userId, CreateTransactionRequest request);
    Transaction createIncome(Long userId, CreateTransactionRequest request);
    List<Transaction> getAllTransactions(Long userId);
}