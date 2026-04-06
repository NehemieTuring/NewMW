package com.mutuelle.controller;

import com.mutuelle.entity.*;
import com.mutuelle.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/member")
@RequiredArgsConstructor
@Tag(name = "Member Portal API", description = "Endpoints for individual members")
@PreAuthorize("hasRole('MEMBER') or hasRole('SUPER_ADMIN')")
public class MemberPortalController {

    private final MemberService memberService;
    private final SavingService savingService;
    private final BorrowingService borrowingService;
    private final HelpService helpService;
    private final ChatService chatService;
    private final AuthService authService;

    // 4. Profil
    @GetMapping("/profile")
    public ResponseEntity<Member> getProfile() {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(memberService.getMemberByEmail(email)); 
    }

    @PutMapping("/profile")
    public ResponseEntity<Member> updateProfile(@RequestBody com.mutuelle.dto.request.RegisterMemberRequest request) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberService.getMemberByEmail(email);
        return ResponseEntity.ok(memberService.updateMember(member.getId(), request));
    }

    @PutMapping("/profile/password")
    public ResponseEntity<Void> updatePassword(@RequestParam String newPassword) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberService.getMemberByEmail(email);
        authService.updatePassword(member.getUser().getId(), newPassword);
        return ResponseEntity.noContent().build();
    }

    // 4. Statut et dettes
    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberService.getMemberByEmail(email);
        return ResponseEntity.ok(memberService.getMemberStatus(member.getId()));
    }

    @GetMapping("/debts")
    public ResponseEntity<Map<String, Object>> getDebts() {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberService.getMemberByEmail(email);
        return ResponseEntity.ok(memberService.getMemberDebts(member.getId()));
    }

    // 4. Épargne
    @GetMapping("/savings")
    public ResponseEntity<List<Saving>> getMySavings() {
        return ResponseEntity.ok(savingService.getMemberSavings(4L));
    }

    @GetMapping("/savings/balance")
    public ResponseEntity<BigDecimal> getSavingBalance() {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberService.getMemberByEmail(email);
        return ResponseEntity.ok(savingService.getMemberBalance(member.getId()));
    }

    // 4. Emprunts
    @GetMapping("/borrowings")
    public ResponseEntity<List<Borrowing>> getMyBorrowings() {
        return ResponseEntity.ok(borrowingService.getMemberLoans(4L));
    }

    @PostMapping("/borrowings/request")
    public ResponseEntity<Borrowing> requestLoan(@RequestParam BigDecimal amount) {
        return ResponseEntity.ok(borrowingService.requestLoan(4L, amount, null));
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

    @PostMapping("/helps/{id}/contribute")
    public ResponseEntity<Contribution> contributeToHelp(@PathVariable Long id, @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(helpService.contributeToHelp(id, 4L, amount));
    }

    // 4. Communication
    @GetMapping("/members")
    @Operation(summary = "Lister les autres membres (pour chat)")
    public ResponseEntity<List<Member>> getOtherMembers() {
        return ResponseEntity.ok(memberService.getAllMembers());
    }

    @PostMapping("/chat/send")
    public ResponseEntity<ChatMessage> sendMessage(@RequestParam Long receiverId, @RequestParam String content) {
        return ResponseEntity.ok(chatService.sendMessage(4L, receiverId, content));
    }

    @GetMapping("/chat/conversations")
    public ResponseEntity<List<User>> getConversations() {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberService.getMemberByEmail(email);
        return ResponseEntity.ok(chatService.getConversations(member.getUser().getId()));
    }

    @GetMapping("/chat/unread")
    public ResponseEntity<Long> getUnreadCount() {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberService.getMemberByEmail(email);
        return ResponseEntity.ok(chatService.getUnreadCount(member.getUser().getId()));
    }
}
