package com.mutuelle.controller;

import com.mutuelle.dto.request.LoginRequest;
import com.mutuelle.dto.response.JwtResponse;
import com.mutuelle.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user login and auth")
public class AuthController {

    private final AuthService authService;
    private final com.mutuelle.service.MemberService memberService;

    @PostMapping("/login")
    @Operation(summary = "Login an existing user")
    public ResponseEntity<JwtResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.authenticateUser(loginRequest));
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new member (Simplified public endpoint)")
    public ResponseEntity<com.mutuelle.entity.Member> registerMember(@Valid @RequestBody com.mutuelle.dto.request.RegisterMemberRequest request) {
        return ResponseEntity.ok(memberService.register(request));
    }
}
