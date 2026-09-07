package com.fintech.core.auth.service;

import com.fintech.core.auth.dto.UpdatePasswordRequest;
import com.fintech.core.common.dto.UserDto;
import com.fintech.core.auth.entity.User;
import com.fintech.core.response.Response;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

    public interface UserService {

    User getCurrentLoggedInUser();

    Response<Page<UserDto>> getAllUsers(int page, int size);

    Response<?> updatePassword(UpdatePasswordRequest updatePasswordRequest);

    Response<?> uploadProfilePicture(MultipartFile file);

    Response<UserDto> getMyProfile();

    Response<?> logout(String bearerToken);

    Response<?> uploadProfilePictureToS3(MultipartFile file);
}
