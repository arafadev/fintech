package com.fintech.fintech.ledger.repository;

import com.fintech.fintech.ledger.entity.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByTransactionIdOrderByIdAsc(Long transactionId);

    Page<LedgerEntry> findByAccountNumberOrderByRecordedAtDesc(String accountNumber, Pageable pageable);
}
