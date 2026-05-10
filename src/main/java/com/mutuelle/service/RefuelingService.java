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

        Cashbox solidarityCashbox = cashboxRepository.findByName(CashboxName.SOLIDARITY)
                .orElseThrow(() -> new BusinessException("Solidarity cashbox not found"));

        // Get all solidarity outflows for the exercise period
        List<TransactionLog> outflows = transactionLogRepository.findByCashboxId(solidarityCashbox.getId())
                .stream()
                .filter(log -> log.getType() == TransactionType.OUTFLOW && "SOLIDARITY_EXPENDITURE".equals(log.getCategory()))
                .filter(log -> !log.getTransactionDate().isBefore(exercise.getStartDate().atStartOfDay()) && 
                               !log.getTransactionDate().isAfter(exercise.getEndDate().atTime(23, 59, 59)))
                .collect(Collectors.toList());

        BigDecimal totalOutflows = outflows.stream()
                .map(TransactionLog::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Member> allMembers = memberService.getAllMembers();
        java.util.Map<Long, BigDecimal> memberDebts = new java.util.HashMap<>();
        allMembers.forEach(m -> memberDebts.put(m.getId(), BigDecimal.ZERO));

        // Temporal proportional distribution
        for (TransactionLog tx : outflows) {
            LocalDate txDate = tx.getTransactionDate().toLocalDate();
            List<Member> presentMembers = allMembers.stream()
                    .filter(m -> m.getInscriptionDate() != null && !m.getInscriptionDate().isAfter(txDate))
                    .collect(Collectors.toList());

            if (!presentMembers.isEmpty()) {
                BigDecimal share = tx.getAmount().divide(new BigDecimal(presentMembers.size()), 4, RoundingMode.HALF_UP);
                for (Member m : presentMembers) {
                    memberDebts.put(m.getId(), memberDebts.get(m.getId()).add(share));
                }
            }
        }

        Refueling refueling = Refueling.builder()
                .exercise(exercise)
                .administrator(admin)
                .totalOutflows(totalOutflows)
                .eligibleMemberCount(allMembers.size())
                .amountPerMember(allMembers.isEmpty() ? BigDecimal.ZERO : totalOutflows.divide(new BigDecimal(allMembers.size()), 2, RoundingMode.HALF_UP)) // Informative average
                .totalCollected(BigDecimal.ZERO)
                .surplusToInscription(BigDecimal.ZERO)
                .distributionDate(LocalDate.now())
                .status("CALCULATED")
                .build();

        Refueling savedRefueling = refuelingRepository.save(refueling);

        for (Member member : allMembers) {
            BigDecimal debtAmount = memberDebts.get(member.getId()).setScale(2, RoundingMode.HALF_UP);
            
            RefuelingDistribution dist = RefuelingDistribution.builder()
                    .refueling(savedRefueling)
                    .member(member)
                    .amountReceived(debtAmount) // We use amountReceived field to store the debt to pay
                    .inRule(true) // Default to true, status will be checked on payment
                    .build();
            distributionRepository.save(dist);

            // Logic: This amount becomes the new Solidarity Debt or Refueling Debt for the member
            // Depending on the business rule, we might update SolidarityDebt directly or create a new entry
            // For now, we follow the user's lead that it replaces/sets the debt for next exercise
            SolidarityDebt debt = solidarityDebtRepository.findByMemberId(member.getId()).orElse(null);
            if (debt != null) {
                // For the next exercise, the debt is the refueling amount
                debt.setTotalDue(debtAmount);
                debt.setTotalPaid(BigDecimal.ZERO);
                debt.setRemainingDebt(debtAmount);
                debt.setStatus(debtAmount.compareTo(BigDecimal.ZERO) > 0 ? "LATE" : "UP_TO_DATE");
                solidarityDebtRepository.save(debt);
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
