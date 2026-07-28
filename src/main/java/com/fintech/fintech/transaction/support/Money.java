package com.fintech.fintech.transaction.support;

import com.fintech.fintech.exception.BadRequestException;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public final class Money {

    public static final MathContext CTX = new MathContext(19, RoundingMode.HALF_EVEN);
    public static final int SCALE = 2;

    private Money() {
    }

    public static BigDecimal of(BigDecimal value) {
        if (value == null) {
            throw new BadRequestException("error.amount.null");
        }
        return value.setScale(SCALE, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal add(BigDecimal left, BigDecimal right) {
        return of(of(left).add(of(right), CTX));
    }

    public static BigDecimal subtract(BigDecimal left, BigDecimal right) {
        return of(of(left).subtract(of(right), CTX));
    }
}
