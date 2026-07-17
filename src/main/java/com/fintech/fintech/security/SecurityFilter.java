package com.fintech.fintech.security;

// استيراد معالجات الأخطاء من باكيج الـ exceptions بتاعة مشروع الفينتك بتاعك
import com.fintech.fintech.exception.CustomAccessDenialHandler;
import com.fintech.fintech.exception.CustomAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration // بنقول لسبرينج: "يا معلم الكلاس ده مليان Beans وإعدادات، اقراه أول ما تقوم"
@EnableWebSecurity // تفعيل السيكيورتي على الويب عشان يحمي الـ Endpoints والـ APIs بتاعتك
@EnableMethodSecurity // تفعيل الحماية على مستوى الدواب (Methods) يعني تقدر تكتب @PreAuthorize فوق الـ Controllers
@RequiredArgsConstructor // لومبوك هتعمل كونستركتور يحقن الفلاتر والـ Handlers المكتوب جنبها final تلقائياً
public class SecurityFilter {

    // حقن البودي جارد بتاعنا اللي هيفحص التوكن في كل Request
    private final AuthFilter authFilter;
    // حقن كلاس التعامل مع الـ 403 (لما يوزر يحاول يدخل مكان مش من حقه وهو مسجل دخول أصلاً)
    private final CustomAccessDenialHandler customAccessDenialHandler;

    // حقن كلاس التعامل مع الـ 401 (لما يوزر يدخل من غير توكن أصلاً أو بتوكن بايظ)
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Bean // بنعرف الـ Filter Chain الأساسية اللي الطلبات هتمشي فيها
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                // 1. وقف حماية الـ CSRF لأننا شغالين REST API وبنستخدم JWT فمش محتاجينها في حاجة
                .csrf(AbstractHttpConfigurer::disable)

                // 2. شغل إعدادات الـ CORS الافتراضية عشان نسمح للفرونت-إند (أنجولار/رياكت) يكلم الباك-إند براحته
                .cors(Customizer.withDefaults())

                // 3. بنقول لسبرينج: "لو حصلت مصيبة أمنية، انده على رجالتنا بتوع الـ Exception Handling دول يتصرفوا"
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler(customAccessDenialHandler) // بتاع الـ 403 (Forbidden)
                        .authenticationEntryPoint(customAuthenticationEntryPoint) // بتاع الـ 401 (Unauthorized)
                )

                // 4. رسم خريطة المرور: مين يعدي ومين يتفتش؟
                .authorizeHttpRequests(req -> req
                        // أي لينك بيبدأ بـ /api/auth/ (زي الـ login والـ register) عديه يابا "تصريح للجميع" بدون أي توكن
                        .requestMatchers("/api/auth/**").permitAll()
                        // أي لينك تاني في السيستم؟ لا يا حبيبي، لازم يوزر يتفتش ويكون موثق (Authenticated)
                        .anyRequest().authenticated()
                )

                // 5. ضبط إدارة الجلسات: إحنا شغالين STATELESS (بدون كوكيز وسيرفر سيكشن) اعتماداً على الـ JWT فقط
                .sessionManagement(mag -> mag.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 6. الحتة الصايعة: حط البودي جارد بتاعنا (authFilter) على البوابة "قبل" فلتر سبرينج التقليدي للباص واليوزر
                .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class);

        // ابنِ السلسلة دي كلها ورجعها لسبرينج عشان يطبقها على الأبلكيشن
        return httpSecurity.build();
    }


    @Bean // بنسجل الـ Encoder اللي هيشفر الباسوردات بطريقة الـ BCrypt القوية قبل ما تنزل الداتا بيز
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean // بنسجل المدير الكبير (AuthenticationManager) عشان نستخدمه وقت الـ Login لمطابقة اليوزر والباص
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

}