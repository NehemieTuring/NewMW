package com.mutuelle.service;

import com.mutuelle.entity.*;
import java.time.Clock;
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
    private final Clock clock;

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

        // 3. Limit loan based on degressive tiers (Risk Management)
        BigDecimal totalSavings = savingService.getMemberBalance(memberId);
        BigDecimal maxLoan = calculateMaxLoan(totalSavings);
        
        if (requestedAmount.compareTo(maxLoan) > 0) {
            throw new BusinessException(String.format(
                "Le montant demandé (%s XAF) dépasse votre droit d'emprunt. " +
                "Avec une épargne de %s XAF, votre plafond est de %s XAF (selon les paliers de risque).",
                requestedAmount.toPlainString(),
                totalSavings.toPlainString(),
                maxLoan.toPlainString()
            ));
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
                .dueDate(LocalDate.now(clock).plusMonths(3)) // Default 3 months, could be dynamic
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
                .transactionDate(LocalDateTime.now(clock))
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
        
        // 1. Verification of status
        if (borrowing.getStatus() == BorrowingStatus.COMPLETED) {
            throw new BusinessException("Ce prêt est déjà entièrement soldé.");
        }

        // 2. Validation of amount
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Le montant du remboursement doit être supérieur à zéro.");
        }
        
        if (amount.compareTo(borrowing.getRemainingBalance()) > 0) {
            throw new BusinessException("Le montant (" + amount + ") dépasse le reste à payer (" + borrowing.getRemainingBalance() + ").");
        }

        Session activeSession = sessionService.getActiveSession();

        // 3. Update debt
        borrowing.setRemainingBalance(borrowing.getRemainingBalance().subtract(amount));
        
        // 4. Update status if settled
        if (borrowing.getRemainingBalance().compareTo(BigDecimal.ZERO) <= 0) {
            borrowing.setStatus(BorrowingStatus.COMPLETED);
            borrowing.setRemainingBalance(BigDecimal.ZERO);
        }
        borrowingRepository.save(borrowing);

        // Update cashbox
        Cashbox savingCashbox = cashboxRepository.findByName(CashboxName.SAVING).orElseThrow();
        savingCashbox.setBalance(savingCashbox.getBalance().add(amount));
        cashboxRepository.save(savingCashbox);

        Refund refund = Refund.builder()
                .borrowing(borrowing)
                .member(borrowing.getMember())
                .session(activeSession)
                .exercise(activeSession.getExercise())
                .amount(amount)
                .remainingBalance(borrowing.getRemainingBalance())
                .refundDate(LocalDate.now(clock))
                .build();
                
        Refund savedRefund = refundRepository.save(refund);

        // Record log
        TransactionLog log = TransactionLog.builder()
                .transactionDate(LocalDateTime.now(clock))
                .member(borrowing.getMember())
                .cashbox(savingCashbox)
                .type(TransactionType.INFLOW)
                .category("LOAN_REFUND")
                .amount(amount)
                .referenceTable("refund")
                .referenceId(savedRefund.getId())
                .description("Remboursement de prêt pour l'emprunt #" + borrowing.getId())
                .build();
        transactionLogRepository.save(log);

        return savedRefund;
    }

    public List<Refund> getRefundsByBorrowingId(Long borrowingId) {
        return refundRepository.findByBorrowingId(borrowingId);
    }

    /**
     * Calcule le montant maximum qu'un membre peut emprunter selon ses paliers d'épargne.
     * Paliers dégressifs pour limiter le risque :
     * - 0 - 500 000 XAF : 5 fois l'épargne
     * - 500 001 - 1 000 000 XAF : 4 fois
     * - 1 000 001 - 1 500 000 XAF : 3 fois
     * - 1 500 001 - 2 000 000 XAF : 2 fois
     * - Plus de 2 000 000 XAF : 1,5 fois
     */
    public BigDecimal calculateMaxLoan(BigDecimal savings) {
        if (savings == null) return BigDecimal.ZERO;
        
        long s = savings.longValue();
        BigDecimal multiplier;

        if (s <= 500000) {
            multiplier = new BigDecimal("5");
        } else if (s <= 1000000) {
            multiplier = new BigDecimal("4");
        } else if (s <= 1500000) {
            multiplier = new BigDecimal("3");
        } else if (s <= 2000000) {
            multiplier = new BigDecimal("2");
        } else {
            multiplier = new BigDecimal("1.5");
        }

        return savings.multiply(multiplier).setScale(0, java.math.RoundingMode.HALF_UP);
    }
}
