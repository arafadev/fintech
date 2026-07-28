package com.fintech.fintech.notification.delivery;

import com.fintech.fintech.notification.dto.NotificationDto;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;

/**
 * Single responsibility: compose and deliver MIME email (Thymeleaf HTML or plain body).
 * Does not persist notification audit rows.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailDeliveryService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public void deliver(NotificationDto notification) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                mimeMessage,
                MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                StandardCharsets.UTF_8.name()
        );

        helper.setTo(notification.getRecipient());
        helper.setSubject(notification.getSubject());
        helper.setText(resolveBody(notification), true);

        mailSender.send(mimeMessage);
        log.info("Email delivered to {}", notification.getRecipient());
    }

    private String resolveBody(NotificationDto notification) {
        if (notification.getTemplateName() == null) {
            return notification.getBody();
        }
        Context context = new Context();
        context.setVariables(notification.getTemplateVariables());
        return templateEngine.process(notification.getTemplateName(), context);
    }
}
