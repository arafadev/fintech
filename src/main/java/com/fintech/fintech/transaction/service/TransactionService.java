package com.fintech.fintech.transaction.service;

import com.fintech.fintech.response.Response;
import com.fintech.fintech.transaction.dto.TransactionDto;
import com.fintech.fintech.transaction.dto.TransactionRequest;
import jakarta.transaction.Transactional;

import java.util.List;

public interface TransactionService {

    Response<?> createTransaction(TransactionRequest transactionRequest);

    Response<List<TransactionDto>> getTransactionsForMyAccount(String accountNumber, int page, int size);


}
