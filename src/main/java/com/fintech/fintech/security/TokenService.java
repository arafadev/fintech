package com.fintech.fintech.security;

import com.fintech.fintech.auth.entity.User;
import com.fintech.fintech.auth.repository.UserRepository;
import com.fintech.fintech.exception.NotFoundException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;


@Service
public class TokenService {


    @Value("${jwt.secret.string}")
    private String JWT_SECRETE;

    @Value("${jwt.expiration.time}")
    private long EXPIRATION_TIME;


    private SecretKey key;


    @PostConstruct
    private void init() {
        byte[] keyByte = JWT_SECRETE.getBytes(StandardCharsets.UTF_8);
        this.key = new SecretKeySpec(keyByte, "HmacSHA256");
    }


    public String generateToken(String email) {
        return Jwts.builder()
                .subject(email) // بنشيل إيميل اليوزر جوه التوكن
                .issuedAt(new Date(System.currentTimeMillis())) // وقت صناعة التوكن (الآن)
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // وقت انتهاء الصلاحية
                .signWith(key) // بنوقع التوكن بالمفتاح السري بتاعنا عشان محدش يزوره
                .compact(); // بنحول كل ده للنص المشفر النهائي (الـ Token)
    }

    public String getUsernameFromToken(String token) {    // هي البيانات المتخزنة جوه الـ Token (زي الإيميل، تاريخ الانتهاء، إلخ).
        return extractClaims(token, Claims::getSubject); // دالة خاصة (Generic Method) وظيفتها تفتح الـ Token المشفر باستخدام المفتاح السري (key) وتستخرج الحمولة اللي جواه (الـ Payload) وترجع أي جزء من البيانات نطلبه منها.
    }

    private <T> T extractClaims(String token, Function<Claims, T> claimsTFunction) {
        return claimsTFunction.
                apply(Jwts.parser().verifyWith(key).
                        build().
                        parseSignedClaims(token)
                        .getPayload());
    }

    /**
     * الدالة الأساسية اللي بيستخدمها الـ JwtFilter مع كل Request للتحقق من صلاحية الـ JWT Token.
     *
     * تعتبر الـ Token صالحة فقط إذا تحقق الشرطان التاليان:
     * 1- اسم المستخدم (الإيميل) المستخرج من الـ Token يطابق اسم المستخدم الحالي
     *    الموجود داخل UserDetails.
     * 2- الـ Token لم تنتهِ صلاحيتها (Not Expired).
     *
     * @param token الـ JWT Token المراد التحقق منها.
     * @param userDetails بيانات المستخدم الحالي المحملة من قاعدة البيانات.
     * @return true إذا كانت الـ Token صالحة، وإلا false.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = getUsernameFromToken(token); // بتنادي على الدالة السحرية دي وتقولها: "من فضلك، هاتيلي الـ Subject (اللي هو الإيميل) من جوه الـ Token ده".
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        return extractClaims(token, Claims::getExpiration).before(new Date());
    }

}