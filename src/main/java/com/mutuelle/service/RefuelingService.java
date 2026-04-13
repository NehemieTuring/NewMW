package com.mutuelle.service;

import com.mutuelle.entity.*;
import com.mutuelle.enums.CashboxName;
import com.mutuelle.enums.TransactionType;
import com.mutuelle.exception.BusinessException;
import com.mutuelle.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RefuelingService {

    private final RefuelingRepository refuelingRepository;
    private final RefuelingDistributionRepository distributionRepository;
    private final MemberService memberService;
    private final ExerciseService exerciseService;
    private final CashboxRepository cashboxRepository;
    private final TransactionLogRepository transactionLogRepository;
    private final SolidarityDebtRepository solidarityDebtRepository;

    @Transactional
    public Refueling calculateRefueling(Long exerciseId, Administrator admin) {
        Exercise exercise = exerciseService.getAllExercises().stream()
                .filter(e -> e.getId().equals(exerciseId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Exercise not found"));

        if (refuelingRepository.findByExerciseId(exerciseId).isPresent()) {
            throw new BusinessException("Refueling already calculated for this exercise");
        }

        // Logic (simplified)
        // S = Total outflows from Social Fund (Solidarity)
        // N = Number of members in rule
        // S/N per member
        
        Cashbox solidarityCashbox = cashboxRepository.findByName(CashboxName.SOLIDARITY)
                .orElseThrow(() -> new BusinessException("Solidarity cashbox not found"));

        // Sum outflows of category SOLIDARITY from transaction log for period exerciseId
        // BigDecimal totalOutflows = ...
        BigDecimal totalOutflows = transactionLogRepository.findByCashboxId(solidarityCashbox.getId())
                .stream()
                .filter(log -> log.getType() == TransactionType.OUTFLOW && "SOLIDARITY_EXPENDITURE".equals(log.getCategory()))
                .map(TransactionLog::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Member> allMembers = memberService.getAllMembers();
        List<Member> membersInRule = allMembers.stream()
                .filter(m -> {
                    SolidarityDebt debt = solidarityDebtRepository.findByMemberId(m.getId()).orElse(null);
                    return debt != null && "UP_TO_DATE".equals(debt.getStatus());
                })
                .collect(Collectors.toList());

        if (membersInRule.isEmpty()) {
            throw new BusinessException("No members in rule found for refueling calculation");
        }

        BigDecimal amountPerMember = totalOutflows.divide(new BigDecimal(membersInRule.size()), 2, RoundingMode.HALF_UP);

        Refueling refueling = Refueling.builder()
                .exercise(exercise)
                .administrator(admin)
                .totalOutflows(totalOutflows)
                .eligibleMemberCount(membersInRule.size())
                .amountPerMember(amountPerMember)
                .totalCollected(BigDecimal.ZERO)
                .surplusToInscription(BigDecimal.ZERO)
                .distributionDate(LocalDate.now())
                .status("CALCULATED")
                .build();

        Refueling savedRefueling = refuelingRepository.save(refueling);

        // Distribute to all (diminishing debt for non-en-regle, cash for en-regle if applicable, but context says to all)
        for (Member member : allMembers) {
            boolean isInRule = membersInRule.contains(member);
            RefuelingDistribution dist = RefuelingDistribution.builder()
                    .refueling(savedRefueling)
                    .member(member)
                    .amountReceived(amountPerMember) // Logical rule: every member gets the amount but non-regle use it to pay debt
                    .inRule(isInRule)
                    .build();
            distributionRepository.save(dist);

            // Logic: non-regle VOIENT LEUR DETTE DIMINUÉE
            if (!isInRule) {
                SolidarityDebt debt = solidarityDebtRepository.findByMemberId(member.getId()).orElse(null);
                if (debt != null) {
                    debt.setTotalPaid(debt.getTotalPaid().add(amountPerMember));
                    debt.setRemainingDebt(debt.getRemainingDebt().subtract(amountPerMember).max(BigDecimal.ZERO));
                    if (debt.getRemainingDebt().compareTo(BigDecimal.ZERO) <= 0) {
                        debt.setStatus("UP_TO_DATE");
                    }
                    solidarityDebtRepository.save(debt);
                }
            }
        }

        savedRefueling.setStatus("DISTRIBUTED");
        return refuelingRepository.save(savedRefueling);
    }

    public Refueling getRefuelingByExerciseId(Long exerciseId) {
        return refuelingRepository.findByExerciseId(exerciseId)
                .orElseThrow(() -> new BusinessException("Refueling not found for this exercise"));
    }

    @Transactional
    public Refueling distributeRefueling(Long refuelingId) {
        Refueling refueling = refuelingRepository.findById(refuelingId)
                .orElseThrow(() -> new BusinessException("Refueling not found"));
        refueling.setStatus("DISTRIBUTED");
        return refuelingRepository.save(refueling);
    }

    public List<RefuelingDistribution> getDistributions(Long refuelingId) {
        return distributionRepository.findByRefuelingId(refuelingId);
    }
}
