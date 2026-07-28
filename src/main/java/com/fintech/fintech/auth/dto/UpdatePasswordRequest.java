package com.fintech.fintech.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
public class UpdatePasswordRequest {

    @NotBlank(message = "{validation.old_password.required}")
    private String oldPassword;

    @NotBlank(message = "{validation.new_password.required}")
    private String newPassword;

}
