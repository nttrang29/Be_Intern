package com.example.financeapp.service;

import com.example.financeapp.dto.CreateTransactionRequest;
import com.example.financeapp.dto.UpdateTransactionRequest;
import com.example.financeapp.entity.Transaction;

import java.util.List;

public interface TransactionService {
    Transaction createExpense(Long userId, CreateTransactionRequest request);
    Transaction createIncome(Long userId, CreateTransactionRequest request);
    Transaction updateTransaction(Long userId, Long transactionId, UpdateTransactionRequest request);
    void deleteTransaction(Long userId, Long transactionId);
    List<Transaction> getAllTransactions(Long userId);
}