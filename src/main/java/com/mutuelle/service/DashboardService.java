package com.mutuelle.service;

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
    private final CashboxRepository cashboxRepository;
    private final SolidarityDebtRepository solidarityDebtRepository;

    public Map<String, Object> getGlobalStats() {
        Map<String, Object> stats = new HashMap<>();
        
        List<Member> allMembers = memberRepository.findAll();
        stats.put("totalMembers", allMembers.size());
        
        List<Member> activeMembers = allMembers.stream().filter(Member::isActive).collect(Collectors.toList());
        stats.put("activeMembers", activeMembers.size());

        List<Cashbox> cashboxes = cashboxRepository.findAll();
        stats.put("cashboxes", cashboxes);

        List<Member> enRegle = allMembers.stream().filter(m -> {
            var debt = solidarityDebtRepository.findByMemberId(m.getId()).orElse(null);
            return debt != null && "UP_TO_DATE".equals(debt.getStatus());
        }).collect(Collectors.toList());

        stats.put("membersInRule", enRegle.size());
        stats.put("membersNotInRule", allMembers.size() - enRegle.size());

        return stats;
    }

    public List<Member> getMembersInRule() {
        return memberRepository.findAll().stream().filter(m -> {
            var debt = solidarityDebtRepository.findByMemberId(m.getId()).orElse(null);
            return debt != null && "UP_TO_DATE".equals(debt.getStatus());
        }).collect(Collectors.toList());
    }

    public List<Member> getMembersNotInRule() {
        return memberRepository.findAll().stream().filter(m -> {
            var debt = solidarityDebtRepository.findByMemberId(m.getId()).orElse(null);
            return debt == null || !"UP_TO_DATE".equals(debt.getStatus());
        }).collect(Collectors.toList());
    }

    public Map<String, Object> getDailyReport() {
        return getGlobalStats();
    }
}
