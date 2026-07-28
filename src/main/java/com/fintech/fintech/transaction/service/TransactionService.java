package com.fintech.fintech.transaction.service;

import com.fintech.fintech.response.Response;
import com.fintech.fintech.transaction.dto.TransactionDto;
import com.fintech.fintech.transaction.dto.TransactionRequest;
import jakarta.validation.Valid;

import java.util.List;

public interface TransactionService {

    Response<?> createTransaction(@Valid TransactionRequest transactionRequest);

    Response<List<TransactionDto>> getTransactionsForMyAccount(String accountNumber, int page, int size);

}
