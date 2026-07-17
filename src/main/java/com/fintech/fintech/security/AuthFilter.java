package com.fintech.fintech.security;

// استيراد كلاس المستخدم (Entity) من نظام الفينتك الخاص بك
import com.fintech.fintech.auth.entity.User;
// استيراد كلاس معالجة أخطاء الدخول غير المصرح به الذي قمت بإنشائه سابقاً
import com.fintech.fintech.exception.CustomAuthenticationEntryPoint;
// استيراد كلاسات واجهة تصفية الويب لتمرير الطلب عبر الفلاتر
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
// استيراد أدوات مكتبة لومبوك للتسهيل وتقليل الـ Boilerplate code
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// استيراد الكلاسات الأساسية لإدارة الجلسة والمصادقة في سبرينج سيكيورتي
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

// استيراد كلاسات جافا الأساسية للعمل مع النصوص والقوائم والدخل/الخرج
import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;

@Component // تسجيل الكلاس كـ Bean في سبرينج ليتم التعرف عليه وحقنه تلقائياً
@Slf4j // تفعيل مكتبة تسجيل الأحداث (Logging) لطباعة الأخطاء في الكونسول
@RequiredArgsConstructor // إنشاء Constructor تلقائي لحقن الـ Beans المكتوب بجانبها final
public class AuthFilter extends OncePerRequestFilter { // الوراثة من فلتر يضمن التنفيذ مرة واحدة فقط لكل طلب (Request)

    // حقن كلاس خدمات الـ JWT لإنشاء وفحص وفك التوكنات
    private final TokenService tokenService;
    // حقن كلاس معالجة أخطاء المصادقة لإرسال رد الرفض المناسب للعميل
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    // حقن خدمة جلب بيانات المستخدم من قاعدة البيانات بالإيميل
    private final CustomUserDetailsService customUserDetailsService;


    @Override // إعادة تعريف ميثود التصفية الأساسية التي تمر عبرها كل الطلبات
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 1. محاولة استخراج التوكن النظيف (بدون كلمة Bearer) من الـ Header الخاص بالطلب
        String token = getTokenFromRequest(request);

        // إذا وجدنا توكن في الطلب، نبدأ عملية التحقق والمصادقة
        if (token != null) {
            String email;
            try {
                // 2. فك التوكن واستخراج البريد الإلكتروني للمستخدم منه باستخدام كلاس التوكنات
                email = tokenService.getUsernameFromToken(token);
            } catch (Exception e) { // في حال فشل فك التوكن (مثال: توكن معدل، مزور، أو منتهي الصلاحية)

                // طباعة خطأ في سجلات السيرفر (Console) لتوضيح سبب المشكلة
                log.error("Exception occured while extracting username from token");
                // إنشاء كائن استثناء يعبر عن خطأ في بيانات المصادقة مع تمرير رسالة الخطأ الأصلية
                AuthenticationException authenticationException = new BadCredentialsException(e.getMessage());
                // إرسال طلب الخطأ فوراً لكلاس الـ Entry Point ليرد على العميل بـ (401 Unauthorized)
                customAuthenticationEntryPoint.commence(request, response, authenticationException);
                return; // إيقاف تنفيذ الميثود فوراً وعدم تمرير الطلب للـ Controllers
            }

            // 3. الذهاب لقاعدة البيانات لجلب كائن الـ UserDetails (الـ AuthUser) باستخدام الإيميل المستخرج
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

            // 4. التحقق من أن البريد موجود فعلياً وأن التوكن سليم ومطابق لبيانات المستخدم وغير منتهي
            if (StringUtils.hasText(email) && tokenService.isTokenValid(token, userDetails)) {

                // إنشاء كارت المصادقة الداخلي لسبرينج ووضع بيانات المستخدم وصلاحياته (الـ Roles) بداخله
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails, // الـ Principal (المستخدم الحالي)
                        null,        // الباسورد (نمرره null لأننا تحققنا بالتوكن مسبقاً)
                        userDetails.getAuthorities()); // استدعاء دالة الصلاحيات والـ Roles لحفظها في الكارت

                // ربط تفاصيل الـ Request الإضافية (مثل الـ IP والجلسة الحالية) بكارت المصادقة
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                // حفظ كارت المصادقة داخل الـ Context ليصبح المستخدم مسجلاً ومقبولاً في النظام لهذا الطلب
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }


        try {
            // 5. تمرير الطلب للفلتر التالي في السلسلة (Chain) ليصل في النهاية للـ Controller الخاص بك
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            // تسجيل أي خطأ غير متوقع يحدث أثناء معالجة الطلب في المراحل اللاحقة
            log.error(e.getMessage());
        }

    }

    // ميثود مساعدة مخصصة لاستخراج التوكن من الـ Authorization Header وتنظيفه
    private String getTokenFromRequest(HttpServletRequest request) {
        // البحث عن الـ Header المسؤول عن المصادقة واسمه "Authorization"
        String tokenWithBearer = request.getHeader("Authorization");
        // التحقق من وجود الـ Header وأنه يبدأ فعلياً بكلمة "Bearer " الشهيرة بالتوكنات
        if (tokenWithBearer != null && tokenWithBearer.startsWith("Bearer ")) {
            // قص أول 7 حروف (وهي طول كلمة "Bearer ") وإرجاع نص التوكن الصافي فقط
            return tokenWithBearer.substring(7);
        }
        // إرجاع null إذا لم يكن هناك توكن أو الهيدر غير سليم
        return null;
    }
}