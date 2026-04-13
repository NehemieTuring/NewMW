package com.mutuelle.controller;

import com.mutuelle.entity.*;
import com.mutuelle.service.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/president")
@RequiredArgsConstructor
@Tag(name = "President API", description = "Read-only access for the President")
@PreAuthorize("hasAuthority('ROLE_PRESIDENT') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_SECRETAIRE_GENERALE')")
public class PresidentPortalController {

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
    private final AuthService authService;
    private final AdminService adminService;

    @GetMapping("/members")
    public ResponseEntity<List<Member>> getAllMembers() {
        return ResponseEntity.ok(memberService.getAllMembers());
    }

    @GetMapping("/members/{id}")
    public ResponseEntity<Member> getMemberById(@PathVariable Long id) {
        return ResponseEntity.ok(memberService.getMemberById(id));
    }

    @GetMapping("/admins")
    public ResponseEntity<List<Administrator>> getAdmins() {
        return ResponseEntity.ok(adminService.getAllAdmins());
    }

    @GetMapping("/members/{id}/status")
    public ResponseEntity<String> getMemberStatus(@PathVariable Long id) {
        return ResponseEntity.ok(memberService.getMemberStatus(id));
    }

    @GetMapping("/members/{id}/debts")
    public ResponseEntity<List<Map<String, Object>>> getMemberDebts(@PathVariable Long id) {
        return ResponseEntity.ok(memberService.getMemberDebts(id));
    }

    @GetMapping("/solidarity/members/{memberId}/debt")
    public ResponseEntity<SolidarityDebt> getSolidarityDebt(@PathVariable Long memberId) {
        return ResponseEntity.ok(solidarityService.getMemberDebt(memberId));
    }

    @GetMapping("/savings/members/{memberId}")
    public ResponseEntity<List<Saving>> getSavings(@PathVariable Long memberId) {
        return ResponseEntity.ok(savingService.getMemberSavings(memberId));
    }

    @GetMapping("/borrowings")
    public ResponseEntity<List<Borrowing>> getAllLoans() {
        return ResponseEntity.ok(borrowingService.getAllBorrowings());
    }

    @GetMapping("/borrowings/{id}")
    public ResponseEntity<Borrowing> getLoan(@PathVariable Long id) {
        return ResponseEntity.ok(borrowingService.getBorrowingById(id));
    }

    @GetMapping("/helps")
    public ResponseEntity<List<Help>> getAllHelps() {
        return ResponseEntity.ok(helpService.getAllHelps());
    }

    @GetMapping("/helps/{id}")
    public ResponseEntity<Help> getHelp(@PathVariable Long id) {
        return ResponseEntity.ok(helpService.getHelpById(id));
    }

    @GetMapping("/helps/active")
    public ResponseEntity<List<Help>> getActiveHelps() {
        return ResponseEntity.ok(helpService.getActiveHelps());
    }

    @GetMapping("/exercises")
    public ResponseEntity<List<Exercise>> getExercises() {
        return ResponseEntity.ok(exerciseService.getAllExercises());
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<Session>> getSessions() {
        return ResponseEntity.ok(sessionService.getAllSessions());
    }

    @GetMapping("/refueling/exercises/{exerciseId}")
    public ResponseEntity<Refueling> getRefueling(@PathVariable Long exerciseId) {
        return ResponseEntity.ok(refuelingService.getRefuelingByExerciseId(exerciseId));
    }

    @GetMapping("/dashboard/exercises/{exerciseId}")
    public ResponseEntity<Map<String, Object>> getExerciseBilan(@PathVariable Long exerciseId) {
        return ResponseEntity.ok(dashboardService.getExerciseBilan(exerciseId));
    }

    @GetMapping("/dashboard/transactions")
    public ResponseEntity<Map<String, Object>> getGlobalTransactions() {
        return ResponseEntity.ok(dashboardService.getGlobalStats());
    }

    @GetMapping("/dashboard/sessions/{sessionId}")
    public ResponseEntity<Map<String, Object>> getSessionBilan(@PathVariable Long sessionId) {
        return ResponseEntity.ok(dashboardService.getSessionBilan(sessionId));
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

    @GetMapping("/chat/unread")
    public ResponseEntity<Long> getUnreadCount() {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        Administrator admin = adminService.getAdminByEmail(email);
        return ResponseEntity.ok(chatService.getUnreadCount(admin.getUser().getId()));
    }

    @GetMapping("/profile")
    public ResponseEntity<User> getProfile() {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        Administrator admin = adminService.getAdminByEmail(email);
        return ResponseEntity.ok(admin.getUser());
    }

    @PutMapping("/profile/password")
    public ResponseEntity<Void> updatePassword(@RequestParam String newPassword) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        Administrator admin = adminService.getAdminByEmail(email);
        authService.updatePassword(admin.getUser().getId(), newPassword);
        return ResponseEntity.noContent().build();
    }
}
