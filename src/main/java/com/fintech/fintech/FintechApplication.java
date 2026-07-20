package com.fintech.fintech;

import com.fintech.fintech.auth.entity.User;
import com.fintech.fintech.enums.NotificationType;
import com.fintech.fintech.notification.dto.NotificationDto;
import com.fintech.fintech.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@RequiredArgsConstructor
public class FintechApplication {

//	private final NotificationService notificationService;

	public static void main(String[] args) {
		SpringApplication.run(FintechApplication.class, args);
	}


//    @Bean
//    CommandLineRunner runner(){
//        return args -> {
//            NotificationDto notificationDTO = NotificationDto.builder()
//                    .recipient("arafa.dev@gmail.com")
//                    .subject("HEllo testing email")
//                    .body("Hey, this is a test eamil 😁")
//                    .type(NotificationType.EMAIL)
//                    .build();
//
//            notificationService.sendEmail(notificationDTO, new User());
//        };
//    }

}
