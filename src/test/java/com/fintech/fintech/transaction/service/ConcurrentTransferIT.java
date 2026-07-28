package com.fintech.fintech.transaction.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * اختبار تكاملي للسباق على الرصيد (double spending).
 * محتاج DB حقيقية أو Testcontainers + @SpringBootTest — مش Mockito بس.
 * القفل PESSIMISTIC_WRITE في findByAccountNumberForUpdate هو اللي بيمنع الرصيد السالب.
 */
class ConcurrentTransferIT {

    @Test
    @Disabled("يتطلب Testcontainers/H2 + SpringBootTest — يُفعَّل عند إعداد بيئة تكامل")
    void concurrentTransfers_shouldNotAllowNegativeBalance() {
        // خطة التنفيذ لما تتفعل البيئة:
        // 1) حساب بمصدر رصيد 100
        // 2) خيطين يعملوا تحويل 100 في نفس اللحظة
        // 3) واحد ينجح وواحد يفشل بـ InsufficientBalance
        // 4) الرصيد النهائي = 0 مش -100
    }
}
