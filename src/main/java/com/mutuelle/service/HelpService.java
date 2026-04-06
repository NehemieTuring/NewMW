package com.mutuelle.service;

import com.mutuelle.entity.*;
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
public class HelpService {

    private final HelpRepository helpRepository;
    private final HelpTypeRepository helpTypeRepository;
    private final MemberService memberService;
    private final ContributionRepository contributionRepository;

    @Transactional
    public Help createHelp(Long typeId, Long beneficiaryId, BigDecimal targetAmount, Administrator admin) {
        HelpType type = helpTypeRepository.findById(typeId)
                .orElseThrow(() -> new BusinessException("Help type not found"));
        Member beneficiary = memberService.getMemberById(beneficiaryId);

        Help help = Help.builder()
                .helpType(type)
                .member(beneficiary)
                .administrator(admin)
                .unitAmount(type.getDefaultAmount() != null ? type.getDefaultAmount() : BigDecimal.ZERO)
                .targetAmount(targetAmount)
                .limitDate(LocalDateTime.now().plusMonths(1))
                .status("ACTIVE")
                .build();

        return helpRepository.save(help);
    }

    @Transactional
    public Contribution contributeToHelp(Long helpId, Long memberId, BigDecimal amount) {
        Help help = helpRepository.findById(helpId)
                .orElseThrow(() -> new BusinessException("Help request not found"));
        Member member = memberService.getMemberById(memberId);

        if (!"ACTIVE".equals(help.getStatus())) {
            throw new BusinessException("Help request is no longer active");
        }

        Contribution contribution = Contribution.builder()
                .help(help)
                .member(member)
                .amount(amount)
                .status("COMPLETED")
                .build();

        contributionRepository.save(contribution);

        help.setCollectedAmount(help.getCollectedAmount().add(amount));
        if (help.getCollectedAmount().compareTo(help.getTargetAmount()) >= 0) {
            help.setStatus("COMPLETED");
        }
        helpRepository.save(help);

        return contribution;
    }

    public List<Help> getActiveHelps() {
        return helpRepository.findByStatus("ACTIVE");
    }

    @Transactional
    public HelpType createHelpType(String name, String description, BigDecimal defaultAmount) {
        HelpType type = HelpType.builder()
                .name(name)
                .description(description)
                .defaultAmount(defaultAmount)
                .active(true)
                .build();
        return helpTypeRepository.save(type);
    }

    public List<HelpType> getAllHelpTypes() {
        return helpTypeRepository.findAll();
    }

    public List<Help> getAllHelps() {
        return helpRepository.findAll();
    }

    public Help getHelpById(Long id) {
        return helpRepository.findById(id).orElseThrow(() -> new BusinessException("Help not found"));
    }
}
