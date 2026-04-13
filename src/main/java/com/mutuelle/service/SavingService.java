package com.mutuelle.service;

import com.mutuelle.entity.*;
import com.mutuelle.enums.CashboxName;
import com.mutuelle.enums.PaymentType;
import com.mutuelle.enums.TransactionType;
import com.mutuelle.exception.BusinessException;
import com.mutuelle.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SavingService {

    private final SavingRepository savingRepository;
    private final MemberService memberService;
    private final SessionService sessionService;
    private final CashboxRepository cashboxRepository;
    private final TransactionLogRepository transactionLogRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public Saving deposit(Long memberId, BigDecimal amount, Administrator administrator) {
        Member member = memberService.getMemberById(memberId);
        Session activeSession = sessionService.getActiveSession();
        
        Cashbox savingCashbox = cashboxRepository.findByName(CashboxName.SAVING)
                .orElseThrow(() -> new BusinessException("Caisse d'épargne introuvable"));

        // Calculate cumulative total (simplified, better to query DB sum)
        List<Saving> memberSavings = savingRepository.findByMemberId(memberId);
        BigDecimal cumulativeTotal = memberSavings.stream()
                .map(s -> s.getType() == TransactionType.INFLOW ? s.getAmount() : s.getAmount().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(amount);

        Saving saving = Saving.builder()
                .member(member)
                .session(activeSession)
                .administrator(administrator)
                .amount(amount)
                .cumulativeTotal(cumulativeTotal)
                .type(TransactionType.INFLOW)
                .build();

        Saving savedSaving = savingRepository.save(saving);

        // Update cashbox
        savingCashbox.setBalance(savingCashbox.getBalance().add(amount));
        cashboxRepository.save(savingCashbox);

        // Record payment
        Payment payment = Payment.builder()
                .member(member)
                .cashbox(savingCashbox)
                .administrator(administrator)
                .paymentType(PaymentType.SAVING_DEPOSIT)
                .amount(amount)
                .paymentDate(LocalDateTime.now())
                .status("COMPLETED")
                .referenceId(savedSaving.getId())
                .build();
        paymentRepository.save(payment);

        // Record log
        TransactionLog log = TransactionLog.builder()
                .transactionDate(LocalDateTime.now())
                .member(member)
                .cashbox(savingCashbox)
                .type(TransactionType.INFLOW)
                .category("SAVING_DEPOSIT")
                .amount(amount)
                .referenceTable("saving")
                .referenceId(savedSaving.getId())
                .description("Saving deposit")
                .build();
        transactionLogRepository.save(log);

        return savedSaving;
    }

    public List<Saving> getMemberSavings(Long memberId) {
        return savingRepository.findByMemberId(memberId);
    }

    public BigDecimal getMemberBalance(Long memberId) {
        List<Saving> savings = savingRepository.findByMemberId(memberId);
        return savings.stream()
                .map(s -> s.getType() == TransactionType.INFLOW ? s.getAmount() : s.getAmount().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public Saving withdraw(Long memberId, BigDecimal amount, Administrator administrator) {
        BigDecimal balance = getMemberBalance(memberId);
        if (balance.compareTo(amount) < 0) {
            throw new BusinessException("Solde insuffisant");
        }
        
        Member member = memberService.getMemberById(memberId);
        Session activeSession = sessionService.getActiveSession();
        Cashbox savingCashbox = cashboxRepository.findByName(CashboxName.SAVING).orElseThrow();

        Saving saving = Saving.builder()
                .member(member)
                .session(activeSession)
                .administrator(administrator)
                .amount(amount)
                .cumulativeTotal(balance.subtract(amount))
                .type(TransactionType.OUTFLOW)
                .build();

        Saving savedSaving = savingRepository.save(saving);
        savingCashbox.setBalance(savingCashbox.getBalance().subtract(amount));
        cashboxRepository.save(savingCashbox);
        
        return savedSaving;
    }

    public List<Saving> getSavingsBySession(Long sessionId) {
        return savingRepository.findBySessionId(sessionId);
    }
}
