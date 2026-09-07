package com.fintech.core.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResetPasswordRequest {


    public interface Forgot {}

    public interface Reset {}

    @NotBlank(groups = Forgot.class, message = "{validation.email.required}")
    @Email(groups = {Forgot.class, Reset.class}, message = "{validation.email.invalid}")
    private String email;

    @NotBlank(groups = Reset.class, message = "{validation.reset_code.required}")
    @Size(min = 5, max = 64, groups = Reset.class, message = "{validation.reset_code.size}")
    private String code;

    @NotBlank(groups = Reset.class, message = "{validation.new_password.required}")
    @Size(min = 8, max = 30, groups = Reset.class, message = "{validation.password.size}")
    private String newPassword;
}
