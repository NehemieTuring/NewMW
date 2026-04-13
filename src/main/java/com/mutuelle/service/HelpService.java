package com.mutuelle.service;

import com.mutuelle.entity.*;
import com.mutuelle.exception.BusinessException;
import com.mutuelle.enums.CashboxName;
import com.mutuelle.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HelpService {

    private final HelpRepository helpRepository;
    private final HelpTypeRepository helpTypeRepository;
    private final MemberService memberService;
    private final ContributionRepository contributionRepository;
    private final CashboxRepository cashboxRepository;
    private final TransactionService transactionService;
    private final BorrowingService borrowingService;
    private final SolidarityService solidarityService;
    private final com.mutuelle.repository.PenaltyRepository penaltyRepository;

    @Transactional
    public Help createHelp(Long typeId, Long beneficiaryId, BigDecimal targetAmount, Administrator admin) {
        HelpType type = helpTypeRepository.findById(typeId)
                .orElseThrow(() -> new BusinessException("Type d'aide introuvable"));
        Member beneficiary = memberService.getMemberById(beneficiaryId);

        Help help = Help.builder()
                .helpType(type)
                .member(beneficiary)
                .administrator(admin)
                .unitAmount(type.getDefaultAmount() != null ? type.getDefaultAmount() : BigDecimal.ZERO)
                .targetAmount(targetAmount)
                .collectedAmount(BigDecimal.ZERO)
                .limitDate(LocalDateTime.now().plusMonths(1))
                .status("ACTIVE")
                .build();

        Help savedHelp = helpRepository.save(help);

        // Automatic Social Fund (Fonds Social) contribution (e.g., 30% of target amount)
        BigDecimal socialFundPart = targetAmount.multiply(new BigDecimal("0.3")); // 30% standard
        Cashbox solidarityBox = cashboxRepository.findByName(CashboxName.SOLIDARITY)
                .orElseThrow(() -> new BusinessException("Caisse de solidarité introuvable"));

        if (solidarityBox.getBalance().compareTo(socialFundPart) >= 0) {
            Contribution socialContr = Contribution.builder()
                    .help(savedHelp)
                    .amount(socialFundPart)
                    .status("COMPLETED")
                    .description("Contribution Fonds Social")
                    .build();
            contributionRepository.save(socialContr);
            
            savedHelp.setCollectedAmount(socialFundPart);
            helpRepository.save(savedHelp);
            
            // Record deduction from Solidarity Fund
            transactionService.recordTransaction(socialFundPart.negate(), "SOLIDARITY_HELP", "Financement Aide: " + type.getName(), solidarityBox, null);
        }

        return savedHelp;
    }

    @Transactional
    public void disburseHelp(Long helpId, Administrator admin) {
        Help help = helpRepository.findById(helpId).orElseThrow();
        if (!"ACTIVE".equals(help.getStatus()) && !"COMPLETED".equals(help.getStatus())) {
            throw new BusinessException("L'aide ne peut pas être décaissée dans son état actuel");
        }

        Member member = help.getMember();
        BigDecimal totalAmount = help.getCollectedAmount();
        BigDecimal remainingAmount = totalAmount;

        // 1. Repay Solidarity Debts first
        var solidarityDebt = solidarityService.getMemberDebt(member.getId());
        if (solidarityDebt.getRemainingDebt().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal toRepay = remainingAmount.min(solidarityDebt.getRemainingDebt());
            solidarityService.paySolidarity(member.getId(), toRepay, admin);
            remainingAmount = remainingAmount.subtract(toRepay);
        }

        // 2. Repay Active Loans if any
        var activeLoans = borrowingService.getMemberLoans(member.getId()).stream()
                .filter(b -> com.mutuelle.enums.BorrowingStatus.ACTIVE.equals(b.getStatus()))
                .toList();
        for (var loan : activeLoans) {
            if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal debt = loan.getRemainingBalance();
            BigDecimal toRepay = remainingAmount.min(debt);
            borrowingService.recordRefund(loan.getId(), toRepay, admin);
            remainingAmount = remainingAmount.subtract(toRepay);
        }

        help.setStatus("DISBURSED");
        helpRepository.save(help);
        
        // Log final disbursement of remainingAmount to member
        // (Optionally add a log entry for the actual payout to the member)
    }

    @Transactional
    public Contribution contributeToHelp(Long helpId, Long memberId, BigDecimal amount) {
        Help help = helpRepository.findById(helpId)
                .orElseThrow(() -> new BusinessException("Demande d'aide introuvable"));
        Member member = memberService.getMemberById(memberId);

        if (!"ACTIVE".equals(help.getStatus())) {
            throw new BusinessException("La demande d'aide n'est plus active");
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

    public HelpType getHelpTypeById(Long id) {
        return helpTypeRepository.findById(id).orElseThrow(() -> new BusinessException("Type d'aide introuvable"));
    }

    public List<Help> getAllHelps() {
        return helpRepository.findAll();
    }

    public Help getHelpById(Long id) {
        return helpRepository.findById(id).orElseThrow(() -> new BusinessException("Aide introuvable"));
    }
}
