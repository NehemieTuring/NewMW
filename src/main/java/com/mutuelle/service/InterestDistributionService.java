package com.mutuelle.service;

import com.mutuelle.entity.*;
import com.mutuelle.exception.BusinessException;
import com.mutuelle.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InterestDistributionService {

    private final InterestDistributionRepository distributionRepository;
    private final InterestDistributionDetailRepository detailRepository;
    private final SavingRepository savingRepository;
    private final BorrowingRepository borrowingRepository;
    private final SessionService sessionService;

    @Transactional
    public InterestDistribution distributeInterests(Long sessionId, Administrator admin) {
        Session session = sessionService.getAllSessions().stream()
                .filter(s -> s.getId().equals(sessionId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Session not found"));

        if (!distributionRepository.findBySessionId(sessionId).isEmpty()) {
            throw new BusinessException("Interest distribution already performed for this session");
        }

        // Logic
        // 1. Total interests from all loans in this session
        List<Borrowing> sessionLoans = borrowingRepository.findAll().stream()
                .filter(b -> b.getSession().getId().equals(sessionId))
                .collect(Collectors.toList());
        
        BigDecimal totalInterest = sessionLoans.stream()
                .map(Borrowing::getInterestAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Identify savers in this session
        List<Saving> sessionSavings = savingRepository.findBySessionId(sessionId);
        BigDecimal totalSessionSavings = sessionSavings.stream()
                .map(Saving::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalSessionSavings.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("No savings found for interest distribution");
        }

        InterestDistribution distribution = InterestDistribution.builder()
                .borrowing(sessionLoans.isEmpty() ? null : sessionLoans.get(0)) // Reference one loan or none
                .session(session)
                .administrator(admin)
                .totalInterest(totalInterest)
                .distributedAmount(totalInterest)
                .remainingAmount(BigDecimal.ZERO)
                .status("COMPLETED")
                .distributionDate(LocalDateTime.now())
                .build();
        
        InterestDistribution savedDistribution = distributionRepository.save(distribution);

        // 3. Distribute proportionally
        for (Saving saving : sessionSavings) {
            BigDecimal proportion = saving.getAmount().divide(totalSessionSavings, 10, RoundingMode.HALF_UP);
            BigDecimal memberInterest = totalInterest.multiply(proportion).setScale(2, RoundingMode.HALF_UP);

            InterestDistributionDetail detail = InterestDistributionDetail.builder()
                    .distribution(savedDistribution)
                    .member(saving.getMember())
                    .amountReceived(memberInterest)
                    .receivedAt(LocalDateTime.now())
                    .build();
            detailRepository.save(detail);

            // Here we could also update the saving balance by adding the interest
        }

        return savedDistribution;
    }
}
