package com.fintech.fintech.ledger.service;

import com.fintech.fintech.ledger.entity.LedgerEntry;
import com.fintech.fintech.ledger.enums.LedgerEntryType;
import com.fintech.fintech.ledger.repository.LedgerEntryRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
public class LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;


    @Builder
    public record LedgerDraft(
            String accountNumber,
            BigDecimal signedAmount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            LedgerEntryType entryType
    ) {}

    public List<LedgerEntry> appendEntries(
            Long transactionId,
            String correlationId,
            Long actorUserId,
            List<LedgerDraft> drafts
    ) {
        List<LedgerEntry> saved = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (LedgerDraft draft : drafts) {
            LedgerEntry entry = LedgerEntry.builder()
                    .accountNumber(draft.accountNumber())
                    .amount(draft.signedAmount())
                    .balanceBefore(draft.balanceBefore())
                    .balanceAfter(draft.balanceAfter())
                    .entryType(draft.entryType())
                    .transactionId(transactionId)
                    .correlationId(correlationId)
                    .actorUserId(actorUserId)
                    .recordedAt(now)
                    .build();

            saved.add(ledgerEntryRepository.save(entry));
        }
        return saved;
    }
}
