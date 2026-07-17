package com.fintech.fintech.notification.service;

import com.fintech.fintech.auth.entity.User;
import com.fintech.fintech.notification.dto.NotificationDto;
import com.fintech.fintech.notification.entity.Notification;
import com.fintech.fintech.enums.NotificationType;
import com.fintech.fintech.notification.repository.NotificationRepository;

// استيراد كلاسات الـ Mail الخاصة بجاكارتا وسبرينج
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

// استيراد كلاسات محرك القوالب Thymeleaf لتوليد إيميلات الـ HTML الملونة
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

// استيراد ترميز النصوص القياسي لجافا
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepo;
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Override
    @Async // عشان الإيميل يتبعت في الخلفية والعميل ميحسش بـ لودنج أو تأخير
    public void sendEmail(NotificationDto notificationDTO, User user) {

        try {
            // 1. تجهيز المظروف الخام للإيميل
            MimeMessage mimeMessage = mailSender.createMimeMessage();

            // 2. استخدام الـ Helper لضبط إعدادات المظروف والترميز ودعم الـ HTML والمرفقات
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            // 3. تحديد المستلم وعنوان الرسالة
            helper.setTo(notificationDTO.getRecipient());
            helper.setSubject(notificationDTO.getSubject());

            // 4. التشيك: هل مبعوت قالب ملون HTML ولا نص عادي؟
            if (notificationDTO.getTemplateName() != null) {
                // لو فيه قالب، بنجهز داتا الـ Thymeleaf ونحقنها جواه
                Context context = new Context();
                context.setVariables(notificationDTO.getTemplateVariables());
                String htmlContent = templateEngine.process(notificationDTO.getTemplateName(), context);
                helper.setText(htmlContent, true); // true هنا معناها إن النص عبارة عن HTML
            } else {
                // لو مفيش قالب، بنبعت النص العادي المكتوب جوه الـ Body
                helper.setText(notificationDTO.getBody(), true);
            }

            // 5. إرسال الإيميل فوراً عبر السيرفر
            mailSender.send(mimeMessage);
            log.info("Email sent Out");

            // 6. حفظ نسخة من الإشعار في أرشيف الداتا بيز (Audit Log) حماية للسيستم
            Notification notificationToSave = Notification.builder()
                    .recipient(notificationDTO.getRecipient())
                    .subject(notificationDTO.getSubject())
                    .body(notificationDTO.getBody())
                    .type(NotificationType.EMAIL)
                    .user(user)
                    .build();

            notificationRepo.save(notificationToSave);

        } catch (MessagingException e) {
            // قفش أي مشكلة سفلية في إرسال البريد وطباعتها في اللوجز بدون تعطيل السيرفر
            log.error("Error while sending email: {}", e.getMessage());
        }
    }
}