package com.fintech.core.auth.service;

import com.fintech.core.auth.dto.LoginRequest;
import com.fintech.core.auth.dto.LoginResponse;
import com.fintech.core.auth.dto.RegisterRequest;
import com.fintech.core.auth.dto.ResetPasswordRequest;
import com.fintech.core.response.Response;

public interface AuthService {
    Response<String> register (RegisterRequest request);
    Response<LoginResponse> login(LoginRequest request);
    Response<? > forgetPassword(String email);
    Response<? > updatePasswordViaResetCode(ResetPasswordRequest resetPasswordRequest);
}
