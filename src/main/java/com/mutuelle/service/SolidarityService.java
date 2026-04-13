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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SolidarityService {

    private final SolidarityRepository solidarityRepository;
    private final SolidarityDebtRepository solidarityDebtRepository;
    private final MemberService memberService;
    private final CashboxRepository cashboxRepository;
    private final TransactionLogRepository transactionLogRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public Solidarity paySolidarity(Long memberId, BigDecimal amount, Administrator administrator) {
        Member member = memberService.getMemberById(memberId);
        
        Cashbox solidarityCashbox = cashboxRepository.findByName(CashboxName.SOLIDARITY)
                .orElseThrow(() -> new BusinessException("Caisse de solidarité introuvable"));

        Solidarity solidarity = Solidarity.builder()
                .member(member)
                .administrator(administrator)
                .amount(amount)
                .paymentDate(LocalDate.now())
                .paymentMethod("CASH")
                .build();

        Solidarity savedSolidarity = solidarityRepository.save(solidarity);

        // Update debt record
        SolidarityDebt debt = solidarityDebtRepository.findByMemberId(memberId)
                .orElse(SolidarityDebt.builder()
                        .member(member)
                        .totalDue(new BigDecimal("150000.00"))
                        .totalPaid(BigDecimal.ZERO)
                        .remainingDebt(new BigDecimal("150000.00"))
                        .status("UP_TO_DATE")
                        .build());

        debt.setTotalPaid(debt.getTotalPaid().add(amount));
        debt.setRemainingDebt(debt.getTotalDue().subtract(debt.getTotalPaid()));
        debt.setLastPaymentDate(LocalDate.now());
        if (debt.getRemainingDebt().compareTo(BigDecimal.ZERO) <= 0) {
            debt.setStatus("UP_TO_DATE");
            debt.setRemainingDebt(BigDecimal.ZERO);
        } else {
            debt.setStatus("LATE");
        }
        solidarityDebtRepository.save(debt);

        // Update cashbox
        solidarityCashbox.setBalance(solidarityCashbox.getBalance().add(amount));
        cashboxRepository.save(solidarityCashbox);

        // Record payment
        Payment payment = Payment.builder()
                .member(member)
                .cashbox(solidarityCashbox)
                .administrator(administrator)
                .paymentType(PaymentType.SOLIDARITY)
                .amount(amount)
                .paymentDate(LocalDateTime.now())
                .status("COMPLETED")
                .referenceId(savedSolidarity.getId())
                .build();
        paymentRepository.save(payment);

        // Record log
        TransactionLog log = TransactionLog.builder()
                .transactionDate(LocalDateTime.now())
                .member(member)
                .cashbox(solidarityCashbox)
                .type(TransactionType.INFLOW)
                .category("SOLIDARITY_PAYMENT")
                .amount(amount)
                .referenceTable("solidarity")
                .referenceId(savedSolidarity.getId())
                .description("Solidarity payment")
                .build();
        transactionLogRepository.save(log);

        return savedSolidarity;
    }

    public SolidarityDebt getMemberDebt(Long memberId) {
        return solidarityDebtRepository.findByMemberId(memberId)
                .orElse(SolidarityDebt.builder()
                        .totalDue(BigDecimal.ZERO)
                        .totalPaid(BigDecimal.ZERO)
                        .remainingDebt(BigDecimal.ZERO)
                        .status("UP_TO_DATE")
                        .build());
    }

    public List<Solidarity> getMemberHistory(Long memberId) {
        return solidarityRepository.findByMemberId(memberId);
    }
}
