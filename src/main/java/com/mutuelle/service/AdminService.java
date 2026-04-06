package com.mutuelle.service;

import com.mutuelle.entity.Administrator;
import com.mutuelle.entity.User;
import com.mutuelle.enums.AdminRole;
import com.mutuelle.enums.RoleType;
import com.mutuelle.exception.BusinessException;
import com.mutuelle.repository.AdministratorRepository;
import com.mutuelle.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdministratorRepository adminRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Administrator createAdmin(String name, String firstName, String email, String username, String password, AdminRole role) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new BusinessException("Email already in use");
        }
        if (adminRepository.findByUsername(username).isPresent()) {
            throw new BusinessException("Username already exists");
        }

        User user = User.builder()
                .name(name)
                .firstName(firstName)
                .email(email)
                .password(passwordEncoder.encode(password))
                .type(RoleType.ADMIN)
                .build();
        User savedUser = userRepository.save(user);

        Administrator admin = Administrator.builder()
                .user(savedUser)
                .adminRole(role)
                .username(username)
                .active(true)
                .build();
        return adminRepository.save(admin);
    }

    public List<Administrator> getAllAdmins() {
        return adminRepository.findAll();
    }

    public Administrator getAdminById(Long id) {
        return adminRepository.findById(id).orElseThrow(() -> new BusinessException("Administrator not found"));
    }

    public Administrator getAdminByEmail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new BusinessException("User not found"));
        return adminRepository.findByUser(user).orElseThrow(() -> new BusinessException("Administrator profile not found"));
    }

    @Transactional
    public void deactivateAdmin(Long id) {
        Administrator admin = getAdminById(id);
        admin.setActive(false);
        adminRepository.save(admin);
    }

    @Transactional
    public void activateAdmin(Long id) {
        Administrator admin = getAdminById(id);
        admin.setActive(true);
        adminRepository.save(admin);
    }

    @Transactional
    public void deleteAdmin(Long id) {
        Administrator admin = getAdminById(id);
        User user = admin.getUser();
        adminRepository.delete(admin);
        userRepository.delete(user);
    }

    @Transactional
    public void updateAdminPassword(Long id, String newPassword) {
        Administrator admin = getAdminById(id);
        User user = admin.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
