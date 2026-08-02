package com.qaassistant.controller;

import com.qaassistant.config.JwtUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtUtil jwtUtil;

    // Allowed developer credentials
    private static final Map<String, String> DEVELOPERS = Map.of(
            "Barath", "Test@123",
            "Rishabh", "Test@123",
            "Whiskey", "Test@123"
    );

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        private String developerName;
        private String password;
    }

    @Data
    @AllArgsConstructor
    public static class LoginResponse {
        private String token;
        private String developerName;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request.getDeveloperName() == null || request.getPassword() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Developer name and password are required"));
        }

        String validPassword = DEVELOPERS.get(request.getDeveloperName());
        if (validPassword != null && validPassword.equals(request.getPassword())) {
            String token = jwtUtil.generateToken(request.getDeveloperName());
            return ResponseEntity.ok(new LoginResponse(token, request.getDeveloperName()));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid developer credentials"));
    }
}
