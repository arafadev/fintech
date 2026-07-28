package com.fintech.fintech.auth.service.impl;

import com.fintech.fintech.auth.dto.UpdatePasswordRequest;
import com.fintech.fintech.auth.entity.User;
import com.fintech.fintech.auth.repository.UserRepository;
import com.fintech.fintech.auth.service.UserService;
import com.fintech.fintech.auth.storage.ProfilePictureStorage;
import com.fintech.fintech.auth.support.CurrentUserResolver;
import com.fintech.fintech.auth.support.ProfilePictureValidator;
import com.fintech.fintech.aws.S3Service;
import com.fintech.fintech.common.dto.UserDto;
import com.fintech.fintech.common.i18n.Messages;
import com.fintech.fintech.common.util.AfterCommit;
import com.fintech.fintech.exception.BadRequestException;
import com.fintech.fintech.notification.dto.NotificationDto;
import com.fintech.fintech.notification.service.NotificationService;
import com.fintech.fintech.response.Response;
import com.fintech.fintech.security.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    private final TokenService tokenService;
    private final ProfilePictureStorage profilePictureStorage;
    private final ProfilePictureValidator profilePictureValidator;
    private final CurrentUserResolver currentUserResolver;
    private final Messages messages;
    private final S3Service s3Service;

    @Override
    public User getCurrentLoggedInUser() {
        return currentUserResolver.auth();
    }

    @Override
    public Response<UserDto> getMyProfile() {
        User user = currentUserResolver.auth();
        return Response.ok(messages.get("success.profile_retrieved"), modelMapper.map(user, UserDto.class));
    }

    @Override
    public Response<Page<UserDto>> getAllUsers(int page, int size) {
        Page<UserDto> userDtos = userRepository.findAll(PageRequest.of(page, size))
                .map(user -> modelMapper.map(user, UserDto.class));
        return Response.ok(messages.get("success.users_retrieved"), userDtos);
    }

    @Override
    public Response<?> updatePassword(UpdatePasswordRequest updatePasswordRequest) {
        User user = currentUserResolver.auth();

        String newPassword = updatePasswordRequest.getNewPassword();
        String oldPassword = updatePasswordRequest.getOldPassword();

        if (oldPassword == null || newPassword == null) {
            throw new BadRequestException("error.auth.old_new_password_required");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BadRequestException("error.auth.old_password_incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        user.setTokenVersion(user.getTokenVersion() == null ? 1L : user.getTokenVersion() + 1);
        userRepository.save(user);

        Map<String, Object> templateVariables = new HashMap<>();
        templateVariables.put("name", user.getFirstName());

        NotificationDto notificationDTO = NotificationDto.builder()
                .recipient(user.getEmail())
                .subject("Your Password Was Successfully Changed")
                .templateName("password-change")
                .templateVariables(templateVariables)
                .build();

        AfterCommit.run(() -> notificationService.sendEmail(notificationDTO, user));

        return Response.ok(messages.get("success.password_changed"));
    }

    @Override
    public Response<?> logout(String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            throw new BadRequestException("error.auth.missing_authorization");
        }
        tokenService.revokeToken(bearerToken.substring(7));
        return Response.ok(messages.get("success.logout"));
    }

    @Override
    public Response<?> uploadProfilePicture(MultipartFile file) {
        profilePictureValidator.validate(file);
        User user = currentUserResolver.auth();
        try {
            String fileUrl = profilePictureStorage.store(file, user.getProfilePictureUrl());
            user.setProfilePictureUrl(fileUrl);
            userRepository.save(user);
            return Response.ok(messages.get("success.profile_picture_uploaded"), fileUrl);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public Response<?> uploadProfilePictureToS3(MultipartFile file){

        log.info("Inside uploadProfilePictureToS3()");
        profilePictureValidator.validate(file);
        User user = getCurrentLoggedInUser();

        try {

            if(user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().isEmpty()){
                s3Service.deleteFile(user.getProfilePictureUrl());
            }
            String s3Url = s3Service.uploadFile(file, "profile-pictures");

            log.info("profile url is: {}", s3Url );

            user.setProfilePictureUrl(s3Url);
            userRepository.save(user);

            return Response.builder()
                    .statusCode(HttpStatus.OK.value())
                    .message("Profile picture uploaded successfully.")
                    .data(s3Url)
                    .build();

        }catch (IOException e){

            throw new RuntimeException(e.getMessage());
        }
    }
}
