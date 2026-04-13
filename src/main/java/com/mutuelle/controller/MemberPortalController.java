package com.mutuelle.controller;

import com.mutuelle.entity.*;
import com.mutuelle.service.*;
import com.mutuelle.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/member")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Member Portal API", description = "Endpoints for individual members")
@PreAuthorize("hasAnyAuthority('ROLE_MEMBER', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_PRESIDENT', 'ROLE_TRESORIER', 'ROLE_SECRETAIRE_GENERALE')")
public class MemberPortalController {

    private final MemberService memberService;
    private final SavingService savingService;
    private final BorrowingService borrowingService;
    private final HelpService helpService;
    private final ChatService chatService;
    private final AuthService authService;
    private final SessionService sessionService;
    private final ExerciseService exerciseService;
    private final UserRepository userRepository;

    private Member getCurrentMemberOrVirtual() {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            return memberService.getMemberByEmail(email);
        } catch (Exception e) {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new com.mutuelle.exception.BusinessException("Utilisateur non trouvé: " + email));
            log.info("Generating virtual profile for admin: {} (Role: {})", email, user.getType());
            
            return Member.builder()
                    .id(0L) 
                    .user(user)
                    .registrationNumber(user.getType().toString())
                    .username(user.getEmail())
                    .inscriptionDate(java.time.LocalDate.now())
                    .active(true)
                    .build();
        }
    }

    // 4. Profil
    @GetMapping("/profile")
    public ResponseEntity<Member> getProfile() {
        return ResponseEntity.ok(getCurrentMemberOrVirtual());
    }

    @PutMapping("/profile")
    public ResponseEntity<Member> updateProfile(@RequestParam String name, @RequestParam String firstName, 
                                              @RequestParam String username, @RequestParam String tel, 
                                              @RequestParam String address) {
        Member member = getCurrentMemberOrVirtual();
        if (member.getId() == 0L) {
             User user = member.getUser();
             user.setName(name);
             user.setFirstName(firstName);
             user.setTel(tel);
             user.setAddress(address);
             userRepository.save(user);
             return ResponseEntity.ok(member);
        }
        return ResponseEntity.ok(memberService.updateProfile(member.getId(), name, firstName, username, tel, address));
    }

    @PutMapping("/profile/password")
    public ResponseEntity<Void> updatePassword(@RequestParam String newPassword) {
        Member member = getCurrentMemberOrVirtual();
        authService.updatePassword(member.getUser().getId(), newPassword);
        return ResponseEntity.noContent().build();
    }

    // 4. Statut et dettes
    private final PaymentService paymentService;

    @GetMapping("/payments")
    public ResponseEntity<List<Payment>> getMyPayments() {
        Member member = getCurrentMemberOrVirtual();
        if (member.getId() == 0L) return ResponseEntity.ok(java.util.Collections.emptyList());
        return ResponseEntity.ok(paymentService.getMemberPayments(member.getId()));
    }

    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        Member member = getCurrentMemberOrVirtual();
        if (member.getId() == 0L) return ResponseEntity.ok("ADMIN_ACTIVE");
        return ResponseEntity.ok(memberService.getMemberStatus(member.getId()));
    }

    @GetMapping("/debts")
    public ResponseEntity<List<Map<String, Object>>> getDebts() {
        Member member = getCurrentMemberOrVirtual();
        if (member.getId() == 0L) return ResponseEntity.ok(java.util.Collections.emptyList());
        return ResponseEntity.ok(memberService.getMemberDebts(member.getId()));
    }

    // 4. Épargne
    @GetMapping("/savings")
    public ResponseEntity<List<Saving>> getMySavings() {
        Member member = getCurrentMemberOrVirtual();
        if (member.getId() == 0L) return ResponseEntity.ok(java.util.Collections.emptyList());
        return ResponseEntity.ok(savingService.getMemberSavings(member.getId()));
    }

    @GetMapping("/savings/balance")
    public ResponseEntity<BigDecimal> getSavingBalance() {
        Member member = getCurrentMemberOrVirtual();
        if (member.getId() == 0L) return ResponseEntity.ok(BigDecimal.ZERO);
        return ResponseEntity.ok(savingService.getMemberBalance(member.getId()));
    }

    // 4. Emprunts
    @GetMapping("/borrowings")
    public ResponseEntity<List<Borrowing>> getMyBorrowings() {
        Member member = getCurrentMemberOrVirtual();
        if (member.getId() == 0L) return ResponseEntity.ok(java.util.Collections.emptyList());
        return ResponseEntity.ok(borrowingService.getMemberLoans(member.getId()));
    }

    @PostMapping("/borrowings/request")
    public ResponseEntity<Borrowing> requestLoan(@RequestParam BigDecimal amount) {
        Member member = getCurrentMemberOrVirtual();
        if (member.getId() == 0L) throw new com.mutuelle.exception.BusinessException("Administrators cannot request loans.");
        return ResponseEntity.ok(borrowingService.requestLoan(member.getId(), amount, null));
    }

    @GetMapping("/borrowings/{id}")
    
    public ResponseEntity<Borrowing> getLoanDetails(@PathVariable Long id) {
        return ResponseEntity.ok(borrowingService.getBorrowingById(id));
    }

    @GetMapping("/borrowings/{id}/refunds")
    
    public ResponseEntity<List<Refund>> getLoanRefunds(@PathVariable Long id) {
        return ResponseEntity.ok(borrowingService.getRefundsByBorrowingId(id));
    }

    // 4. Aides
    @GetMapping("/helps/types")
    
    public ResponseEntity<List<HelpType>> getHelpTypes() {
        return ResponseEntity.ok(helpService.getAllHelpTypes());
    }

    @GetMapping("/helps/active")
    
    public ResponseEntity<List<Help>> getActiveHelps() {
        return ResponseEntity.ok(helpService.getActiveHelps());
    }

    @GetMapping("/helps/{id}")
    
    public ResponseEntity<Help> getHelpDetails(@PathVariable Long id) {
        return ResponseEntity.ok(helpService.getHelpById(id));
    }

    @PostMapping("/helps")
    public ResponseEntity<Help> requestHelp(@RequestParam Long typeId, @RequestParam(required = false) BigDecimal amount) {
        Member member = getCurrentMemberOrVirtual();
        if (member.getId() == 0L) throw new com.mutuelle.exception.BusinessException("Administrators cannot request help.");
        
        HelpType type = helpService.getHelpTypeById(typeId);
        BigDecimal targetAmount = (amount != null) ? amount : type.getDefaultAmount();
        
        return ResponseEntity.ok(helpService.createHelp(typeId, member.getId(), targetAmount, null));
    }

    @PostMapping("/helps/{id}/contribute")
    public ResponseEntity<Contribution> contributeToHelp(@PathVariable Long id, @RequestParam BigDecimal amount) {
        Member member = getCurrentMemberOrVirtual();
        if (member.getId() == 0L) throw new com.mutuelle.exception.BusinessException("Administrators cannot contribute to help via member portal.");
        return ResponseEntity.ok(helpService.contributeToHelp(id, member.getId(), amount));
    }

    // 4. Communication
    @GetMapping("/members")
    @Operation(summary = "Lister tous les membres et administrateurs (pour chat)")
    public ResponseEntity<List<User>> getOtherMembers() {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(userRepository.findAll().stream()
                .filter(u -> !u.getEmail().equals(email))
                .filter(u -> u.getType() != com.mutuelle.enums.RoleType.SUPER_ADMIN)
                .distinct()
                .collect(java.util.stream.Collectors.toList()));
    }

    @PostMapping("/chat/send")
    public ResponseEntity<ChatMessage> sendMessage(@RequestParam Long receiverId, @RequestParam String content) {
        Member member = getCurrentMemberOrVirtual();
        return ResponseEntity.ok(chatService.sendMessage(member.getUser().getId(), receiverId, content));
    }

    @GetMapping("/chat/conversations")
    public ResponseEntity<List<User>> getConversations() {
        Member member = getCurrentMemberOrVirtual();
        return ResponseEntity.ok(chatService.getConversations(member.getUser().getId()));
    }

    @GetMapping("/chat/messages/{userId}")
    public ResponseEntity<List<ChatMessage>> getMessages(@PathVariable Long userId) {
        Member member = getCurrentMemberOrVirtual();
        return ResponseEntity.ok(chatService.getMessages(member.getUser().getId(), userId));
    }

    @GetMapping("/chat/unread")
    public ResponseEntity<Long> getUnreadCount() {
        Member member = getCurrentMemberOrVirtual();
        return ResponseEntity.ok(chatService.getUnreadCount(member.getUser().getId()));
    }

    @PutMapping("/chat/mark-read/{messageId}")
    
    public ResponseEntity<Void> markRead(@PathVariable Long messageId) {
        chatService.markAsRead(messageId);
        return ResponseEntity.noContent().build();
    }

    // 4. Sessions et Exercices
    @GetMapping("/sessions")
    
    public ResponseEntity<List<Session>> getSessions() {
        return ResponseEntity.ok(sessionService.getAllSessions());
    }

    @GetMapping("/exercises")
    
    public ResponseEntity<List<Exercise>> getExercises() {
        return ResponseEntity.ok(exerciseService.getAllExercises());
    }
}
