package com.mutuelle.controller;

import com.mutuelle.dto.request.RegisterMemberRequest;
import com.mutuelle.entity.*;
import com.mutuelle.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Secrétaire Générale API", description = "Endpoints for operational management")
// @PreAuthorize removed from class level to allow debug endpoint access
public class AdminPortalController {

    private final MemberService memberService;
    private final SolidarityService solidarityService;
    private final SavingService savingService;
    private final BorrowingService borrowingService;
    private final HelpService helpService;
    private final ExerciseService exerciseService;
    private final SessionService sessionService;
    private final RefuelingService refuelingService;
    private final DashboardService dashboardService;
    private final ChatService chatService;
    private final AdminService adminService;
    private final AuthService authService;
    private final AgapeService agapeService;

    // 3.1 Membres
    @PostMapping("/members")
    @Operation(summary = "Ajouter un membre (inscription)")
    public ResponseEntity<Member> registerMember(@Valid @RequestBody RegisterMemberRequest request) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        Administrator currentAdmin = adminService.getAdminByEmail(email);
        request.setAdminId(currentAdmin.getId());
        if (request.getRegistrationNumber() == null || request.getRegistrationNumber().isBlank()) {
            request.setRegistrationNumber("MEM-" + System.currentTimeMillis());
        }
        return ResponseEntity.ok(memberService.register(request));
    }

    @GetMapping("/members")
    @Operation(summary = "Lister tous les membres")
    public ResponseEntity<List<Member>> getAllMembers() {
        return ResponseEntity.ok(memberService.getAllMembers());
    }

    @GetMapping("/members/{id}")
    public ResponseEntity<Member> getMemberById(@PathVariable Long id) {
        return ResponseEntity.ok(memberService.getMemberById(id));
    }

    @PutMapping("/members/{id}")
    public ResponseEntity<Member> updateMember(@PathVariable Long id, @Valid @RequestBody RegisterMemberRequest request) {
        return ResponseEntity.ok(memberService.updateMember(id, request));
    }

    @PutMapping("/members/{id}/deactivate")
    public ResponseEntity<Void> deactivateMember(@PathVariable Long id) {
        memberService.deactivateMember(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/members/{id}/activate")
    public ResponseEntity<Void> activateMember(@PathVariable Long id) {
        memberService.activateMember(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/members/{id}/status")
    public ResponseEntity<String> getMemberStatus(@PathVariable Long id) {
        return ResponseEntity.ok(memberService.getMemberStatus(id));
    }

    @GetMapping("/members/{id}/debts")
    public ResponseEntity<List<Map<String, Object>>> getMemberDebts(@PathVariable Long id) {
        return ResponseEntity.ok(memberService.getMemberDebts(id));
    }

    // 3.1 Solidarité
    @PostMapping("/solidarity/payments")
    public ResponseEntity<Solidarity> recordSolidarityPayment(@RequestParam Long memberId, @RequestParam BigDecimal amount) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        Administrator admin = adminService.getAdminByEmail(email);
        return ResponseEntity.ok(solidarityService.paySolidarity(memberId, amount, admin));
    }

    @GetMapping("/solidarity/members/{memberId}/debt")
    public ResponseEntity<SolidarityDebt> getSolidarityDebt(@PathVariable Long memberId) {
        return ResponseEntity.ok(solidarityService.getMemberDebt(memberId));
    }

    @GetMapping("/solidarity/members/{memberId}/history")
    public ResponseEntity<List<Solidarity>> getSolidarityHistory(@PathVariable Long memberId) {
        return ResponseEntity.ok(solidarityService.getMemberHistory(memberId));
    }

    // 3.1 Épargne
    @PostMapping("/savings/deposit")
    public ResponseEntity<Saving> deposit(@RequestParam Long memberId, @RequestParam BigDecimal amount) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        Administrator admin = adminService.getAdminByEmail(email);
        return ResponseEntity.ok(savingService.deposit(memberId, amount, admin));
    }

    @PostMapping("/savings/withdrawal")
    public ResponseEntity<Saving> withdraw(@RequestParam Long memberId, @RequestParam BigDecimal amount) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        Administrator admin = adminService.getAdminByEmail(email);
        return ResponseEntity.ok(savingService.withdraw(memberId, amount, admin));
    }

    @GetMapping("/savings/members/{memberId}")
    public ResponseEntity<List<Saving>> getSavings(@PathVariable Long memberId) {
        return ResponseEntity.ok(savingService.getMemberSavings(memberId));
    }

    @GetMapping("/savings/members/{memberId}/balance")
    public ResponseEntity<BigDecimal> getSavingBalance(@PathVariable Long memberId) {
        return ResponseEntity.ok(savingService.getMemberBalance(memberId));
    }

    @GetMapping("/savings/sessions/{sessionId}")
    public ResponseEntity<List<Saving>> getSavingsBySession(@PathVariable Long sessionId) {
        return ResponseEntity.ok(savingService.getSavingsBySession(sessionId));
    }

    // 3.1 Emprunts
    @PostMapping("/borrowings/request")
    public ResponseEntity<Borrowing> requestLoan(@RequestParam Long memberId, @RequestParam BigDecimal amount) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        Administrator admin = adminService.getAdminByEmail(email);
        return ResponseEntity.ok(borrowingService.requestLoan(memberId, amount, admin));
    }

    @GetMapping("/borrowings")
    public ResponseEntity<List<Borrowing>> getAllLoans() {
        return ResponseEntity.ok(borrowingService.getAllBorrowings());
    }

    @GetMapping("/borrowings/{id}")
    public ResponseEntity<Borrowing> getLoan(@PathVariable Long id) {
        return ResponseEntity.ok(borrowingService.getBorrowingById(id));
    }

    @PostMapping("/borrowings/{id}/refund")
    public ResponseEntity<Refund> refundLoan(@PathVariable Long id, @RequestParam BigDecimal amount) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        Administrator admin = adminService.getAdminByEmail(email);
        return ResponseEntity.ok(borrowingService.recordRefund(id, amount, admin));
    }

    @GetMapping("/borrowings/members/{memberId}")
    public ResponseEntity<List<Borrowing>> getMemberLoans(@PathVariable Long memberId) {
        return ResponseEntity.ok(borrowingService.getMemberLoans(memberId));
    }

    // Agape
    @PostMapping("/agapes")
    public ResponseEntity<Agape> createAgape(@RequestParam String title, @RequestParam String description, @RequestParam BigDecimal amount, @RequestParam String date, @RequestParam Long sessionId) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        Administrator admin = adminService.getAdminByEmail(email);
        return ResponseEntity.ok(agapeService.createAgape(title, description, amount, java.time.LocalDate.parse(date), sessionId, admin));
    }

    @GetMapping("/agapes")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_SECRETAIRE_GENERALE', 'ROLE_PRESIDENT', 'ROLE_TRESORIER')")
    public ResponseEntity<List<Agape>> getAgapes() {
        return ResponseEntity.ok(agapeService.getAllAgapes());
    }

    @PostMapping("/helps/{id}/disburse")
    public ResponseEntity<Void> disburseHelp(@PathVariable Long id) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        Administrator admin = adminService.getAdminByEmail(email);
        helpService.disburseHelp(id, admin);
        return ResponseEntity.noContent().build();
    }

    // 3.1 Aides
    @PostMapping("/helps/types")
    public ResponseEntity<HelpType> createHelpType(@RequestParam String name, @RequestParam String description, @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(helpService.createHelpType(name, description, amount));
    }

    @GetMapping("/helps/types")
    public ResponseEntity<List<HelpType>> getHelpTypes() {
        return ResponseEntity.ok(helpService.getAllHelpTypes());
    }

    @PostMapping("/helps")
    public ResponseEntity<Help> createHelp(@RequestParam Long typeId, @RequestParam Long beneficiaryId, @RequestParam(required = false) BigDecimal amount) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        Administrator admin = adminService.getAdminByEmail(email);
        
        HelpType type = helpService.getHelpTypeById(typeId);
        BigDecimal targetAmount = (amount != null) ? amount : type.getDefaultAmount();
        
        return ResponseEntity.ok(helpService.createHelp(typeId, beneficiaryId, targetAmount, admin));
    }

    @GetMapping("/helps")
    public ResponseEntity<List<Help>> getAllHelps() {
        return ResponseEntity.ok(helpService.getAllHelps());
    }

    @GetMapping("/helps/{id}")
    public ResponseEntity<Help> getHelp(@PathVariable Long id) {
        return ResponseEntity.ok(helpService.getHelpById(id));
    }

    @PostMapping("/helps/{id}/contribute")
    public ResponseEntity<Contribution> contributeToHelp(@PathVariable Long id, @RequestParam Long memberId, @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(helpService.contributeToHelp(id, memberId, amount));
    }

    @GetMapping("/helps/active")
    public ResponseEntity<List<Help>> getActiveHelps() {
        return ResponseEntity.ok(helpService.getActiveHelps());
    }

    // Sessions et Exercices
    @PostMapping("/exercises")
    public ResponseEntity<?> createExercise(@RequestBody Exercise exercise) {
        try {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return ResponseEntity.status(401).body(java.util.Map.of("message", "Vous devez être connecté pour créer un exercice."));
            }
            
            String email = auth.getName();
            System.out.println("=== EXERCISE CREATION DEBUG ===");
            System.out.println("User email: " + email);
            System.out.println("Exercise year: " + exercise.getYear());
            System.out.println("Exercise startDate: " + exercise.getStartDate());
            System.out.println("Exercise endDate: " + exercise.getEndDate());
            
            Administrator admin = adminService.getAdminByEmail(email);
            System.out.println("Admin found: " + admin.getId() + " - " + admin.getUsername());
            
            Exercise created = exerciseService.createExercise(exercise, admin);
            System.out.println("Exercise created successfully with ID: " + created.getId());
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            System.err.println("=== EXERCISE CREATION FAILED ===");
            e.printStackTrace();
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return ResponseEntity.status(500).body(java.util.Map.of("message", "Erreur lors de la création: " + msg));
        }
    }


    @GetMapping("/exercises")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_SECRETAIRE_GENERALE', 'ROLE_PRESIDENT', 'ROLE_TRESORIER')")
    public ResponseEntity<List<Exercise>> getExercises() {
        return ResponseEntity.ok(exerciseService.getAllExercises());
    }

    @PutMapping("/exercises/{id}/close")
    public ResponseEntity<Void> closeExercise(@PathVariable Long id) {
        exerciseService.closeExercise(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/exercises/{id}")
    public ResponseEntity<Exercise> updateExercise(@PathVariable Long id, @RequestBody Exercise exercise) {
        return ResponseEntity.ok(exerciseService.updateExercise(id, exercise));
    }

    @GetMapping("/exercises/current")
    public ResponseEntity<Exercise> getCurrentExercise() {
        return ResponseEntity.ok(exerciseService.findActiveExercise().orElse(null));
    }

    @PostMapping("/sessions")
    public ResponseEntity<Session> createSession(@RequestBody Session session) {
        return ResponseEntity.ok(sessionService.createSession(session));
    }

    @GetMapping("/sessions")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_SECRETAIRE_GENERALE', 'ROLE_PRESIDENT', 'ROLE_TRESORIER')")
    public ResponseEntity<List<Session>> getSessions() {
        return ResponseEntity.ok(sessionService.getAllSessions());
    }

    @PutMapping("/sessions/{id}/close")
    public ResponseEntity<Session> closeSession(@PathVariable Long id) {
        return ResponseEntity.ok(sessionService.closeSession(id));
    }

    @PutMapping("/sessions/{id}/configure")
    public ResponseEntity<Session> configureSession(@PathVariable Long id, @RequestBody Map<String, Object> config) {
        return ResponseEntity.ok(sessionService.configureSession(id, config));
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long id) {
        sessionService.deleteSession(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/exercises/{id}")
    public ResponseEntity<Void> deleteExercise(@PathVariable Long id) {
        exerciseService.deleteExercise(id);
        return ResponseEntity.noContent().build();
    }

    // Renflouement
    @PostMapping("/refueling/calculate/{exerciseId}")
    public ResponseEntity<Refueling> calculateRefueling(@PathVariable Long exerciseId) {
        return ResponseEntity.ok(refuelingService.calculateRefueling(exerciseId, null));
    }

    @GetMapping("/refueling/exercises/{exerciseId}")
    public ResponseEntity<Refueling> getRefueling(@PathVariable Long exerciseId) {
        return ResponseEntity.ok(refuelingService.getRefuelingByExerciseId(exerciseId));
    }

    @PostMapping("/refueling/distribute/{refuelingId}")
    public ResponseEntity<Refueling> distributeRefueling(@PathVariable Long refuelingId) {
        return ResponseEntity.ok(refuelingService.distributeRefueling(refuelingId));
    }

    // Bilans
    @GetMapping("/dashboard/exercises/{exerciseId}")
    public ResponseEntity<Map<String, Object>> getExerciseBilan(@PathVariable Long exerciseId) {
        return ResponseEntity.ok(dashboardService.getExerciseBilan(exerciseId));
    }

    @GetMapping("/dashboard/sessions/{sessionId}")
    public ResponseEntity<Map<String, Object>> getSessionBilan(@PathVariable Long sessionId) {
        return ResponseEntity.ok(dashboardService.getSessionBilan(sessionId));
    }

    @GetMapping("/dashboard/transactions")
    public ResponseEntity<Map<String, Object>> getGlobalTransactions() {
        return ResponseEntity.ok(dashboardService.getGlobalStats());
    }

    @GetMapping("/dashboard/cashboxes")
    public ResponseEntity<Map<String, Object>> getCashboxes() {
        return ResponseEntity.ok(dashboardService.getGlobalStats());
    }

    @GetMapping("/dashboard/members/in-rule")
    public ResponseEntity<List<Member>> getMembersInRule() {
        return ResponseEntity.ok(dashboardService.getMembersInRule());
    }

    @GetMapping("/dashboard/members/not-in-rule")
    public ResponseEntity<List<Member>> getMembersNotInRule() {
        return ResponseEntity.ok(dashboardService.getMembersNotInRule());
    }

    // Chat / Communication
    @PostMapping("/chat/send")
    public ResponseEntity<ChatMessage> sendMessage(@RequestParam Long receiverId, @RequestParam String content) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        Administrator admin = adminService.getAdminByEmail(email);
        return ResponseEntity.ok(chatService.sendMessage(admin.getUser().getId(), receiverId, content));
    }

    @GetMapping("/chat/conversations")
    public ResponseEntity<List<User>> getConversations() {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        Administrator admin = adminService.getAdminByEmail(email);
        return ResponseEntity.ok(chatService.getConversations(admin.getUser().getId()));
    }

    @GetMapping("/chat/messages/{userId}")
    public ResponseEntity<List<ChatMessage>> getMessages(@PathVariable Long userId) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        Administrator admin = adminService.getAdminByEmail(email);
        return ResponseEntity.ok(chatService.getMessages(admin.getUser().getId(), userId));
    }

    @GetMapping("/chat/unread")
    public ResponseEntity<Long> getUnreadCount() {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        Administrator admin = adminService.getAdminByEmail(email);
        return ResponseEntity.ok(chatService.getUnreadCount(admin.getUser().getId()));
    }

    @PutMapping("/chat/mark-read/{messageId}")
    public ResponseEntity<Void> markRead(@PathVariable Long messageId) {
        chatService.markAsRead(messageId);
        return ResponseEntity.noContent().build();
    }

    // Profil Administrateur
    @GetMapping("/profile")
    public ResponseEntity<Administrator> getProfile() {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(adminService.getAdminByEmail(email));
    }

    @PutMapping("/profile")
    public ResponseEntity<Administrator> updateProfile(@RequestParam String name, @RequestParam String firstName, @RequestParam String username) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        Administrator admin = adminService.getAdminByEmail(email);
        return ResponseEntity.ok(adminService.updateProfile(admin.getId(), name, firstName, username));
    }

    @PutMapping("/profile/password")
    public ResponseEntity<Void> updatePassword(@RequestParam String newPassword) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        Administrator admin = adminService.getAdminByEmail(email);
        authService.updatePassword(admin.getUser().getId(), newPassword);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admins")
    public ResponseEntity<List<Administrator>> getOtherAdmins() {
        return ResponseEntity.ok(adminService.getAllAdmins());
    }

    @GetMapping("/debug/roles")
    public ResponseEntity<List<String>> getDebugRoles() {
        return ResponseEntity.ok(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                .collect(java.util.stream.Collectors.toList()));
    }
}
