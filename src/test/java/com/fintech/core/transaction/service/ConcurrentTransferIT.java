package com.fintech.core.transaction.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class ConcurrentTransferIT {

    @Test
    @Disabled("Requires Testcontainers/H2 + SpringBootTest — enable when integration env is ready")
    void concurrentTransfers_shouldNotAllowNegativeBalance() {
    }
}
