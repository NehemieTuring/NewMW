package com.mutuelle.controller;

import com.mutuelle.entity.Administrator;
import com.mutuelle.enums.AdminRole;
import com.mutuelle.service.AdminService;
import com.mutuelle.service.AuthService;
import com.mutuelle.service.DashboardService;
import com.mutuelle.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/super")
@RequiredArgsConstructor
@Tag(name = "Super Admin API", description = "Endpoints restricted to Super Admins")
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
public class SuperAdminController {

    private final AdminService adminService;
    private final MemberService memberService;
    private final AuthService authService;
    private final DashboardService dashboardService;

    // Gestion des administrateurs
    @PostMapping("/admins")
    @Operation(summary = "Ajouter un administrateur")
    public ResponseEntity<Administrator> addAdmin(@RequestParam String name, @RequestParam String firstName, 
                                                @RequestParam String email, @RequestParam String username, 
                                                @RequestParam String password, @RequestParam AdminRole role) {
        return ResponseEntity.ok(adminService.createAdmin(name, firstName, email, username, password, role));
    }

    @GetMapping("/admins")
    @Operation(summary = "Lister tous les administrateurs")
    public ResponseEntity<List<Administrator>> getAllAdmins() {
        return ResponseEntity.ok(adminService.getAllAdmins());
    }

    @GetMapping("/admins/{id}")
    @Operation(summary = "Consulter le profil d’un administrateur")
    public ResponseEntity<Administrator> getAdminById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getAdminById(id));
    }

    @PutMapping("/admins/{id}/deactivate")
    @Operation(summary = "Désactiver un administrateur")
    public ResponseEntity<Void> deactivateAdmin(@PathVariable Long id) {
        adminService.deactivateAdmin(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/admins/{id}/activate")
    @Operation(summary = "Activer un administrateur")
    public ResponseEntity<Void> activateAdmin(@PathVariable Long id) {
        adminService.activateAdmin(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/admins/{id}")
    @Operation(summary = "Supprimer un administrateur")
    public ResponseEntity<Void> deleteAdmin(@PathVariable Long id) {
        adminService.deleteAdmin(id);
        return ResponseEntity.noContent().build();
    }

    // Gestion des membres
    @DeleteMapping("/members/{id}")
    @Operation(summary = "Supprimer définitivement un membre")
    public ResponseEntity<Void> deleteMember(@PathVariable Long id) {
        memberService.deleteMember(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/users/password")
    @Operation(summary = "Changer le mot de passe d’un utilisateur par son email")
    public ResponseEntity<Void> changeUserPasswordByEmail(@RequestParam String email, @RequestParam String newPassword) {
        authService.updatePasswordByEmail(email, newPassword);
        return ResponseEntity.noContent().build();
    }

    // Dashboard
    @GetMapping("/dashboard")
    @Operation(summary = "Accès complet au tableau de bord")
    public ResponseEntity<Map<String, Object>> getFullDashboard() {
        return ResponseEntity.ok(dashboardService.getGlobalStats());
    }
}
