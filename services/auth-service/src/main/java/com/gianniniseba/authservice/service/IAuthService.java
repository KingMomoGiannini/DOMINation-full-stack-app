package com.gianniniseba.authservice.service;

import com.gianniniseba.authservice.dto.AuthResponse;
import com.gianniniseba.authservice.dto.LoginRequest;
import com.gianniniseba.authservice.dto.RegisterRequest;

public interface IAuthService {

    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);

}
