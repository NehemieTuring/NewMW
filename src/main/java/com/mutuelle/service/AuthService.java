package com.mutuelle.service;

import com.mutuelle.config.JwtTokenProvider;
import com.mutuelle.dto.request.LoginRequest;
import com.mutuelle.dto.response.JwtResponse;
import com.mutuelle.entity.User;
import com.mutuelle.repository.AdministratorRepository;
import com.mutuelle.repository.MemberRepository;
import com.mutuelle.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final AdministratorRepository administratorRepository;
    private final MemberRepository memberRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getIdentifier(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = tokenProvider.generateToken(authentication);

            String userEmail = authentication.getName();
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur introuvable après authentification : " + userEmail));

            String subRole = null;
            if (user.getType() == com.mutuelle.enums.RoleType.ADMIN) {
                subRole = administratorRepository.findByUser(user)
                        .map(admin -> admin.getAdminRole().name())
                        .orElse(null);
            }

            return new JwtResponse(user.getId(), jwt, user.getEmail(), user.getEmail(), user.getType().name(), subRole);
        } catch (org.springframework.security.authentication.BadCredentialsException ex) {
            throw new com.mutuelle.exception.UnauthorizedException("Identifiants incorrects");
        }
    }

    public void updatePassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public void updatePasswordByEmail(String identifier, String newPassword) {
        User user = userRepository.findByEmail(identifier).orElse(null);
        if (user == null) {
            user = memberRepository.findByUsername(identifier)
                    .map(com.mutuelle.entity.Member::getUser)
                    .orElse(null);
        }
        if (user == null) {
            user = administratorRepository.findByUsername(identifier)
                    .map(com.mutuelle.entity.Administrator::getUser)
                    .orElse(null);
        }

        if (user == null) {
            throw new RuntimeException("Utilisateur non trouvé : " + identifier);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
