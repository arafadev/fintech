package com.fintech.fintech.auth.service;

import com.fintech.fintech.auth.dto.LoginRequest;
import com.fintech.fintech.auth.dto.LoginResponse;
import com.fintech.fintech.auth.dto.RegisterRequest;
import com.fintech.fintech.auth.dto.ResetPasswordRequest;
import com.fintech.fintech.response.Response;

public interface AuthService {
    Response<String> register (RegisterRequest request);
    Response<LoginResponse> login(LoginRequest request);
    Response<? > forgetPassword(String email);
    Response<? > updatePasswordViaResetCode(ResetPasswordRequest resetPasswordRequest);
}
