package com.fintech.fintech.exception;

import com.fintech.fintech.response.Response;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
@RequiredArgsConstructor

public class CustomAuthenticationEntryPoint implements  AuthenticationEntryPoint {

        private final ObjectMapper objectMapper;

        @Override
        public void commence (HttpServletRequest request,
                              HttpServletResponse response,
                              AuthenticationException authenticationException)
                throws IOException, ServletException{

            Response<?> errorResponse = Response.builder().statusCode(HttpStatus.UNAUTHORIZED.value())
                    .message(authenticationException.getMessage()).build();

            response.setContentType("applicationn/json");
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));


        }

}
