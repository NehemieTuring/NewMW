package com.mutuelle.service;

import com.mutuelle.entity.*;
import com.mutuelle.enums.BorrowingStatus;
import com.mutuelle.enums.CashboxName;
import com.mutuelle.enums.TransactionType;
import com.mutuelle.exception.BusinessException;
import com.mutuelle.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BorrowingService {

    private final BorrowingRepository borrowingRepository;
    private final MemberService memberService;
    private final SessionService sessionService;
    private final SavingService savingService;
    private final CashboxRepository cashboxRepository;
    private final TransactionLogRepository transactionLogRepository;
    private final RefundRepository refundRepository;

    @Transactional
    public Borrowing requestLoan(Long memberId, BigDecimal requestedAmount, Administrator administrator) {
        Member member = memberService.getMemberById(memberId);
        Session activeSession = sessionService.getActiveSession();
        
        // Rules
        // 1. One active loan at a time
        List<BorrowingStatus> activeStatuses = List.of(BorrowingStatus.PENDING, BorrowingStatus.ACTIVE);
        if (!borrowingRepository.findByMemberIdAndStatusIn(memberId, activeStatuses).isEmpty()) {
            throw new BusinessException("Member already has an active loan");
        }

        // 2. Limit loan based on savings (3x)
        BigDecimal totalSavings = savingService.getMemberBalance(memberId);
        BigDecimal maxLoan = totalSavings.multiply(new BigDecimal("3.00"));
        if (requestedAmount.compareTo(maxLoan) > 0) {
            throw new BusinessException("Loan amount exceeds 3x your savings. Max: " + maxLoan);
        }

        // 3. Deduction (97% to member, 3% interest)
        BigDecimal interestAmount = requestedAmount.multiply(new BigDecimal("0.03"));
        BigDecimal netAmount = requestedAmount.subtract(interestAmount);

        Borrowing borrowing = Borrowing.builder()
                .member(member)
                .session(activeSession)
                .administrator(administrator)
                .requestedAmount(requestedAmount)
                .approvedAmount(requestedAmount)
                .interestAmount(interestAmount)
                .netAmountReceived(netAmount)
                .remainingBalance(requestedAmount)
                .status(BorrowingStatus.ACTIVE)
                .dueDate(LocalDate.now().plusMonths(3))
                .build();

        Borrowing savedBorrowing = borrowingRepository.save(borrowing);

        // Update cashbox
        Cashbox savingCashbox = cashboxRepository.findByName(CashboxName.SAVING)
                .orElseThrow(() -> new BusinessException("Saving cashbox not found"));
        savingCashbox.setBalance(savingCashbox.getBalance().subtract(netAmount));
        cashboxRepository.save(savingCashbox);

        // Record log
        TransactionLog log = TransactionLog.builder()
                .transactionDate(LocalDateTime.now())
                .member(member)
                .cashbox(savingCashbox)
                .type(TransactionType.OUTFLOW)
                .category("BORROWING_LOAN")
                .amount(netAmount)
                .referenceTable("borrowing")
                .referenceId(savedBorrowing.getId())
                .description("Loan disbursement")
                .build();
        transactionLogRepository.save(log);

        return savedBorrowing;
    }

    public List<Borrowing> getMemberLoans(Long memberId) {
        return borrowingRepository.findByMemberId(memberId);
    }

    public List<Borrowing> getAllBorrowings() {
        return borrowingRepository.findAll();
    }

    public Borrowing getBorrowingById(Long id) {
        return borrowingRepository.findById(id).orElseThrow(() -> new BusinessException("Borrowing not found"));
    }

    @Transactional
    public Refund recordRefund(Long borrowingId, BigDecimal amount, Administrator administrator) {
        Borrowing borrowing = getBorrowingById(borrowingId);
        if (borrowing.getStatus() == BorrowingStatus.COMPLETED) {
            throw new BusinessException("Loan is already fully repaid");
        }

        borrowing.setRemainingBalance(borrowing.getRemainingBalance().subtract(amount).max(BigDecimal.ZERO));
        if (borrowing.getRemainingBalance().compareTo(BigDecimal.ZERO) <= 0) {
            borrowing.setStatus(BorrowingStatus.COMPLETED);
        }
        borrowingRepository.save(borrowing);

        // Update cashbox
        Cashbox savingCashbox = cashboxRepository.findByName(CashboxName.SAVING).orElseThrow();
        savingCashbox.setBalance(savingCashbox.getBalance().add(amount));
        cashboxRepository.save(savingCashbox);

        return Refund.builder()
                .borrowing(borrowing)
                .member(borrowing.getMember())
                .session(borrowing.getSession())
                .exercise(borrowing.getSession().getExercise())
                .amount(amount)
                .remainingBalance(borrowing.getRemainingBalance())
                .refundDate(LocalDate.now())
                .build();
    }

    public List<Refund> getRefundsByBorrowingId(Long borrowingId) {
        return refundRepository.findByBorrowingId(borrowingId);
    }
}
