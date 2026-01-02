package com.example.financeapp.transaction.service;

import com.example.financeapp.transaction.dto.CreateTransactionRequest;
import com.example.financeapp.transaction.dto.UpdateTransactionRequest;
import com.example.financeapp.transaction.entity.Transaction;

import java.util.List;

public interface TransactionService {
    Transaction createExpense(Long userId, CreateTransactionRequest request);
    Transaction createIncome(Long userId, CreateTransactionRequest request);
    Transaction updateTransaction(Long userId, Long transactionId, UpdateTransactionRequest request);
    void deleteTransaction(Long userId, Long transactionId);
    List<Transaction> getAllTransactions(Long userId);
}