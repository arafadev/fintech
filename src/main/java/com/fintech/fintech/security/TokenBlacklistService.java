package com.fintech.fintech.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Blacklist بسيطة في الميموري: لو عملت Logout بنتكلم التوكن الحالية لحد ما تنتهي صلاحيتها.
 * ملاحظة إنتربرايز: في الإنتاج الأفضل Redis عشان يشتغل على أكتر من instance.
 */
@Service
@Slf4j
public class TokenBlacklistService {

    // key = hash(token) ، value = وقت انتهاء صلاحية التوكن (millis)
    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    // بنحط التوكن في القائمة السوداء لحد ما الـ JWT نفسها تنتهي (مش أكتر من كده)
    public void blacklist(String token, long expiresAtEpochMs) {
        blacklist.put(hash(token), expiresAtEpochMs);
        log.info("Token blacklisted until {}", expiresAtEpochMs);
    }

    public boolean isBlacklisted(String token) {
        Long expiresAt = blacklist.get(hash(token));
        if (expiresAt == null) {
            return false;
        }
        // لو خلصت مدة التوكن أصلاً، نشيلها من الماب ونعتبرها مش blacklisted (انتهت لوحدها)
        if (expiresAt < System.currentTimeMillis()) {
            blacklist.remove(hash(token));
            return false;
        }
        return true;
    }

    // تنضيف دوري للمدخلات المنتهية عشان الماب متكبرش على الفاضي
    @Scheduled(fixedRate = 3_600_000)
    public void cleanupExpired() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Long>> it = blacklist.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> entry = it.next();
            if (entry.getValue() < now) {
                it.remove();
            }
        }
    }

    // مش بنخزّن التوكن خام — بنخزّن SHA-256 عشان لو حد وصل للميموري مياخدش التوكن نفسه
    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
