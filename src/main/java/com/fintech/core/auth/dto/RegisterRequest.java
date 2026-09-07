package com.fintech.core.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RegisterRequest {

    @NotBlank(message = "{validation.first_name.required}")
    @Size(min = 2, max = 50, message = "{validation.first_name.size}")
    private String firstName;

    @NotBlank(message = "{validation.last_name.required}")
    @Size(min = 2, max = 50, message = "{validation.last_name.size}")
    private String lastName;

    @NotBlank(message = "{validation.phone.required}")
    @Pattern(regexp = "^01[0125]\\d{8}$", message = "{validation.phone.pattern}")
    private String phoneNumber;

    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.invalid}")
    @Size(max = 100, message = "{validation.email.size}")
    private String email;


    @NotBlank(message = "{validation.password.required}")
    @Size(min = 8, max = 30, message = "{validation.password.size}")
    private String password;
}
