    package com.fintech.fintech.auth.service.impl;

    import com.fintech.fintech.auth.dto.*;
    import com.fintech.fintech.auth.entity.PasswordResetCode;
    import com.fintech.fintech.auth.entity.User;
    import com.fintech.fintech.auth.repository.PasswordResetCodeRepository;
    import com.fintech.fintech.auth.repository.UserRepository;
    import com.fintech.fintech.auth.service.AuthService;
    import com.fintech.fintech.auth.service.CodeGenerator;
    import com.fintech.fintech.auth.service.UserService;
    import com.fintech.fintech.enums.AccountType;
    import com.fintech.fintech.enums.Currency;
    import com.fintech.fintech.exception.BadRequestException;
    import com.fintech.fintech.exception.NotFoundException;
    import com.fintech.fintech.notification.dto.NotificationDto;
    import com.fintech.fintech.notification.entity.Notification;
    import com.fintech.fintech.notification.service.NotificationService;
    import com.fintech.fintech.response.Response;
    import com.fintech.fintech.role.entity.Role;
    import com.fintech.fintech.role.repository.RoleRepository;
    import com.fintech.fintech.security.TokenService;
    import jakarta.transaction.Transactional;
    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;
    import org.modelmapper.ModelMapper;
    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.PageRequest;
    import org.springframework.http.HttpStatus;
    import org.springframework.security.core.Authentication;
    import org.springframework.security.core.context.SecurityContextHolder;
    import org.springframework.security.crypto.password.PasswordEncoder;
    import org.springframework.stereotype.Service;
    import org.springframework.web.multipart.MultipartFile;

    import java.io.IOException;
    import java.nio.file.Files;
    import java.nio.file.Path;
    import java.nio.file.Paths;
    import java.time.LocalDateTime;
    import java.util.*;
    import java.util.stream.Collectors;

    @Service
    @RequiredArgsConstructor
    @Slf4j
    public class UserServiceImpl implements UserService {

        private final UserRepository userRepository;
        private final NotificationService notificationService;
        private final PasswordEncoder passwordEncoder;
        private final ModelMapper modelMapper;

        // this path for backend
//        private final String uploadDir = "J:/spring/spring_projects/fintech/public/profile-picture/";
        // this path for saving image to fintech-reat project
        private final String uploadDir = "J:/spring/spring_projects/fintech-react/public/profile-picture/";

        @Override
        public User getCurrentLoggedInUser() {

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if(authentication == null ){
                throw  new NotFoundException("User is not authentication");
            }
            String email = authentication.getName();
            return userRepository.findByEmail(email).orElseThrow(()-> new NotFoundException("User not found"));

        }

        @Override
            public Response<UserDto> getMyProfile() {

            User user = getCurrentLoggedInUser();

            UserDto userDto = modelMapper.map(user, UserDto.class);

            return Response.<UserDto>builder()
                    .statusCode(HttpStatus.OK.value())
                    .message("User retrieved")
                    .data(userDto)
                    .build();

        }

        @Override
        public Response<Page<UserDto>> getAllUsers(int page, int size){

            Page<User> users = userRepository.findAll(PageRequest.of(page, size));

            Page<UserDto> userDtos = users.map(user->modelMapper.map(user, UserDto.class));

            return Response.<Page<UserDto>>builder()
                    .statusCode(HttpStatus.OK.value())
                    .message("user retrieved")
                    .data(userDtos)
                    .build();
        }

        @Override
        public Response<?> updatePassword(UpdatePasswordRequest updatePasswordRequest) {
            User user = getCurrentLoggedInUser();

            String newPassword = updatePasswordRequest.getNewPassword();
            String oldPassword = updatePasswordRequest.getOldPassword();

            if (oldPassword == null || newPassword == null) {
                throw new BadRequestException("Old and New Password Required");
            }

            // Validate the old password.
            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                throw new BadRequestException("Old Password not Correct");
            }
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setUpdatedAt(LocalDateTime.now());

            userRepository.save(user);


            // Send password change confirmation email.
            Map<String, Object> templateVariables = new HashMap<>();
            templateVariables.put("name", user.getFirstName());

            NotificationDto notificationDTO = NotificationDto.builder()
                    .recipient(user.getEmail())
                    .subject("Your Password Was Successfully Changed")
                    .templateName("password-change")
                    .templateVariables(templateVariables)
                    .build();

            notificationService.sendEmail(notificationDTO, user);

            return Response.builder()
                    .statusCode(HttpStatus.OK.value())
                    .message("Password Changed Successfully")
                    .build();

        }

        @Override
        public Response<?> uploadProfilePicture(MultipartFile file) {
            User user = getCurrentLoggedInUser();

            try {
                Path uploadPath = Paths.get(uploadDir);

                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                if (user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().isEmpty()) {
                    Path oldFile = Paths.get(user.getProfilePictureUrl());
                    if (Files.exists(oldFile)) {
                        Files.delete(oldFile);
                    }
                }


                // Generate a unique file name to avoid conflicts
                String originalFileName = file.getOriginalFilename();
                String fileExtension = "";
                if (originalFileName != null && originalFileName.contains(".")) {
                    fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
                }

                String newFileName = UUID.randomUUID() + fileExtension;
                Path filePath = uploadPath.resolve(newFileName);

                Files.copy(file.getInputStream(), filePath);

//            String fileUrl = uploadDir + newFileName; //this is for backend
                String fileUrl =  "profile-picture/" + newFileName;// this is the relative path from the frontend


                user.setProfilePictureUrl(fileUrl);
                userRepository.save(user);

                return Response.builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Profile picture uploaded successfully.")
                        .data(fileUrl)
                        .build();

            } catch (IOException e) {
                throw new RuntimeException(e.getMessage());
            }
        }

//        @Override
//        public Response<?> uploadProfilePictureToS3(MultipartFile file){
//
//            log.info("Inside uploadProfilePictureToS3()");
//            User user = getCurrentLoggedInUser();
//
//            try {
//
//                if(user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().isEmpty()){
//                    s3Service.deleteFile(user.getProfilePictureUrl());
//                }
//                String s3Url = s3Service.uploadFile(file, "profile-pictures");
//
//                log.info("profile url is: {}", s3Url );
//
//                user.setProfilePictureUrl(s3Url);
//                userRepo.save(user);
//
//                return Response.builder()
//                        .statusCode(HttpStatus.OK.value())
//                        .message("Profile picture uploaded successfully.")
//                        .data(s3Url)
//                        .build();
//
//            }catch (IOException e){
//
//                throw new RuntimeException(e.getMessage());
//            }
//        }

    }
