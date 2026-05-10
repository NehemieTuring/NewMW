package com.mutuelle.service;

import com.mutuelle.entity.Administrator;
import com.mutuelle.entity.Cashbox;
import com.mutuelle.entity.Member;
import com.mutuelle.exception.BusinessException;
import com.mutuelle.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final MemberRepository memberRepository;
    private final AdministratorRepository administratorRepository;
    private final UserRepository userRepository;
    private final CashboxRepository cashboxRepository;
    private final BorrowingRepository borrowingRepository;
    private final TransactionLogRepository transactionLogRepository;
    private final SessionRepository sessionRepository;
    private final SolidarityDebtRepository solidarityDebtRepository;
    private final MemberService memberService;

    public Map<String, Object> getGlobalStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // Count all unique participants (Members + Admins)
        long totalMembers = userRepository.findAll().stream()
                .filter(u -> u.getType() != com.mutuelle.enums.RoleType.SUPER_ADMIN)
                .count();
        stats.put("totalMembers", totalMembers);
        
        List<Member> allMembers = memberRepository.findAll();
        long activeParticipants = allMembers.stream().filter(Member::isActive).count();
        stats.put("activeMembers", activeParticipants);

        // Financial Totals from Cashboxes
        List<Cashbox> cashboxes = cashboxRepository.findAll();
        stats.put("cashboxes", cashboxes);
        
        java.math.BigDecimal globalBalance = cashboxes.stream()
                .map(Cashbox::getBalance)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        stats.put("globalBalance", globalBalance);

        stats.put("totalEnrollments", cashboxes.stream()
                .filter(c -> c.getName() == com.mutuelle.enums.CashboxName.INSCRIPTION)
                .map(Cashbox::getBalance)
                .findFirst().orElse(java.math.BigDecimal.ZERO));

        stats.put("totalSocialFunds", cashboxes.stream()
                .filter(c -> c.getName() == com.mutuelle.enums.CashboxName.SOLIDARITY)
                .map(Cashbox::getBalance)
                .findFirst().orElse(java.math.BigDecimal.ZERO));

        stats.put("totalSavings", cashboxes.stream()
                .filter(c -> c.getName() == com.mutuelle.enums.CashboxName.SAVING)
                .map(Cashbox::getBalance)
                .findFirst().orElse(java.math.BigDecimal.ZERO));

        // Total Loans (outstanding principal)
        java.math.BigDecimal totalLoans = borrowingRepository.findAll().stream()
                .map(com.mutuelle.entity.Borrowing::getApprovedAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        stats.put("totalLoans", totalLoans);

        // Total Refunds
        stats.put("totalRefunds", transactionLogRepository.findAll().stream()
                .filter(tx -> "REFUND".equals(tx.getCategory()) || "LOAN_REFUND".equals(tx.getCategory()))
                .map(tx -> tx.getAmount())
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));

        // Total Penalties
        stats.put("totalPenalties", transactionLogRepository.findAll().stream()
                .filter(tx -> "PENALTY".equals(tx.getCategory()))
                .map(tx -> tx.getAmount())
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));

        // Recent Transactions
        stats.put("recentTransactions", transactionLogRepository.findTop10ByOrderByTransactionDateDesc().stream()
                .map(tx -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", tx.getId());
                    m.put("date", tx.getTransactionDate());
                    m.put("type", tx.getCategory()); 
                    m.put("description", tx.getDescription());
                    m.put("amount", tx.getAmount());
                    return m;
                }).collect(Collectors.toList()));

        // Active Session
        sessionRepository.findByActiveTrue().ifPresent(s -> {
            Map<String, Object> sMap = new HashMap<>();
            sMap.put("id", s.getId());
            sMap.put("name", s.getName() != null ? s.getName() : "Session #" + s.getSessionNumber());
            sMap.put("sessionDate", s.getDate());
            sMap.put("exerciseYear", s.getExercise().getYear());
            stats.put("activeSession", sMap);
        });

        // Rules state
        List<Member> enRegle = allMembers.stream().filter(m -> 
            "EN_REGLE".equals(memberService.getMemberStatus(m.getId()))
        ).collect(Collectors.toList());

        stats.put("membersInRule", enRegle.size());
        stats.put("membersNotInRule", allMembers.size() - enRegle.size());

        return stats;
    }

    public List<Member> getMembersInRule() {
        return memberRepository.findAll().stream()
                .filter(m -> "EN_REGLE".equals(memberService.getMemberStatus(m.getId())))
                .collect(Collectors.toList());
    }
    
    public List<Member> getMembersNotInRule() {
        return memberRepository.findAll().stream()
                .filter(m -> !"EN_REGLE".equals(memberService.getMemberStatus(m.getId())))
                .collect(Collectors.toList());
    }

    public Map<String, Object> getExerciseBilan(Long exerciseId) {
        Map<String, Object> stats = getGlobalStats();
        stats.put("exerciseId", exerciseId);
        // Additional exercise-specific logic can go here
        return stats;
    }

    public Map<String, Object> getSessionBilan(Long sessionId) {
        Map<String, Object> stats = getGlobalStats();
        stats.put("sessionId", sessionId);
        // Additional session-specific logic can go here
        return stats;
    }

    public Map<String, Object> getDailyReport() {
        return getGlobalStats();
    }
}
