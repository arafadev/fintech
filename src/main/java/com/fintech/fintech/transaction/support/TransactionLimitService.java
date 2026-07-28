package com.fintech.fintech.transaction.support;

import com.fintech.fintech.account.entity.Account;
import com.fintech.fintech.common.config.AppProperties;
import com.fintech.fintech.exception.BadRequestException;
import com.fintech.fintech.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class TransactionLimitService {

    private final TransactionRepository transactionRepository;
    private final AppProperties appProperties;

    public void assertMaxSingleAmount(BigDecimal amount) {
        BigDecimal max = Money.of(appProperties.getLimits().getMaxSingleAmount());
        if (Money.of(amount).compareTo(max) > 0) {
            throw new BadRequestException("error.limit.max_single", max);
        }
    }

    public void assertDailyOutgoingLimit(Account source, BigDecimal amount) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        BigDecimal todayTotal = transactionRepository
                .sumSuccessfulOutgoingSince(source.getAccountNumber(), startOfDay);

        if (todayTotal == null) {
            todayTotal = BigDecimal.ZERO;
        }
        todayTotal = Money.of(todayTotal);

        BigDecimal projected = Money.add(todayTotal, amount);
        BigDecimal limit = Money.of(appProperties.getLimits().getDailyOutgoing());

        if (projected.compareTo(limit) > 0) {
            throw new BadRequestException("error.limit.daily_outgoing", todayTotal, limit);
        }
    }

    public void assertOutgoingAllowed(Account source, BigDecimal amount) {
        assertMaxSingleAmount(amount);
        assertDailyOutgoingLimit(source, amount);
    }
}
