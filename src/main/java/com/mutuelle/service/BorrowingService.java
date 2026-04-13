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
        Exercise exercise = activeSession.getExercise();
        
        // 1. Member must be active
        if (!member.isActive()) {
            throw new BusinessException("Ce membre n'est pas actif et ne peut pas demander de prêt.");
        }

        // 2. No active loan allowed
        List<BorrowingStatus> activeStatuses = List.of(BorrowingStatus.PENDING, BorrowingStatus.ACTIVE);
        if (!borrowingRepository.findByMemberIdAndStatusIn(memberId, activeStatuses).isEmpty()) {
            throw new BusinessException("Ce membre a déjà un prêt en cours.");
        }

        // 3. Check for overdue debts (Insolvency)
        // For now, check if they have any SolidarityDebt with status != 'UP_TO_DATE' 
        // Or if they have completed loans with remaining balance (already covered by rule 2 for active ones)
        // Here we can check if they have any pending refunds from past (simplified check)

        // 4. Limit loan based on savings (3x)
        BigDecimal totalSavings = savingService.getMemberBalance(memberId);
        BigDecimal maxLoan = totalSavings.multiply(new BigDecimal("3.00"));
        if (requestedAmount.compareTo(maxLoan) > 0) {
            throw new BusinessException("Le montant du prêt dépasse 3 fois votre épargne. Votre épargne: " + totalSavings + ". Prêt maximum autorisé: " + maxLoan);
        }

        // 5. Dynamic Interest Deduction (based on Exercise setting)
        // Interest is pre-deducted: Member receives net, repays gross.
        BigDecimal rate = exercise.getInterestRate().divide(new BigDecimal("100"), 4, java.math.RoundingMode.HALF_UP);
        BigDecimal interestAmount = requestedAmount.multiply(rate);
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
                .dueDate(LocalDate.now().plusMonths(3)) // Default 3 months, could be dynamic
                .build();

        Borrowing savedBorrowing = borrowingRepository.save(borrowing);

        // Update cashbox (Saving cashbox is used for loans)
        Cashbox savingCashbox = cashboxRepository.findByName(CashboxName.SAVING)
                .orElseThrow(() -> new BusinessException("Caisse d'épargne introuvable"));
        
        if (savingCashbox.getBalance().compareTo(netAmount) < 0) {
            throw new BusinessException("Fonds insuffisants dans la Caisse d'Épargne pour décaisser ce prêt.");
        }

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
                .description("Loan disbursement (Interests pre-deducted: " + interestAmount + " XAF)")
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
        return borrowingRepository.findById(id).orElseThrow(() -> new BusinessException("Emprunt introuvable"));
    }

    @Transactional
    public Refund recordRefund(Long borrowingId, BigDecimal amount, Administrator administrator) {
        Borrowing borrowing = getBorrowingById(borrowingId);
        if (borrowing.getStatus() == BorrowingStatus.COMPLETED) {
            throw new BusinessException("Le prêt est déjà entièrement remboursé");
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
