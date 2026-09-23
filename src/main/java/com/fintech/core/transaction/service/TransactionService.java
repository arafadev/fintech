package com.fintech.core.transaction.service;

import com.fintech.core.response.Response;
import com.fintech.core.transaction.dto.TransactionDto;
import com.fintech.core.transaction.dto.TransactionRequest;
import jakarta.validation.Valid;

import java.util.List;

public interface TransactionService {

    Response<?> createTransaction(@Valid TransactionRequest transactionRequest);

    Response<List<TransactionDto>> getTransactionsForMyAccount(String accountNumber, int page, int size);

}
