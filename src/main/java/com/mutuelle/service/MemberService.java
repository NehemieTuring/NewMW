package com.mutuelle.service;

import com.mutuelle.dto.request.RegisterMemberRequest;
import com.mutuelle.entity.*;
import com.mutuelle.enums.CashboxName;
import com.mutuelle.enums.PaymentType;
import com.mutuelle.enums.TransactionType;
import com.mutuelle.enums.RoleType;
import com.mutuelle.exception.BusinessException;
import com.mutuelle.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final AdministratorRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final SolidarityDebtRepository solidarityDebtRepository;
    private final BorrowingRepository borrowingRepository;
    private final CashboxRepository cashboxRepository;
    private final TransactionLogRepository transactionLogRepository;
    private final PaymentRepository paymentRepository;
    private final ExerciseService exerciseService;
    private final RefuelingDistributionRepository refuelingDistributionRepository;

    @Transactional
    public Member register(RegisterMemberRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BusinessException("Cet email est déjà utilisé");
        }
        if (request.getUsername() != null && !request.getUsername().isBlank() && memberRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new BusinessException("Ce nom d'utilisateur existe déjà");
        }

        User user = User.builder()
                .name(request.getName())
                .firstName(request.getFirstName())
                .email(request.getEmail())
                .tel(request.getTel())
                .address(request.getAddress())
                .password(passwordEncoder.encode(request.getPassword()))
                .type(RoleType.MEMBER)
                .build();

        User savedUser = userRepository.save(user);

        Administrator administrator = adminRepository.findById(request.getAdminId())
                .orElseThrow(() -> new BusinessException("Administrateur introuvable"));

        Member member = Member.builder()
                .user(savedUser)
                .administrator(administrator)
                .username(request.getUsername())
                .inscriptionDate(request.getInscriptionDate())
                .active(true)
                .build();

        Member savedMember = memberRepository.save(member);
        
        // Initializing Solidarity Debt for the new member
        // New members start with zero debt for past events
        SolidarityDebt initialDebt = SolidarityDebt.builder()
                .member(savedMember)
                .totalDue(BigDecimal.ZERO)
                .totalPaid(BigDecimal.ZERO)
                .remainingDebt(BigDecimal.ZERO)
                .status("UP_TO_DATE")
                .build();
        solidarityDebtRepository.save(initialDebt);

        // Record Registration Fee Payment automatically
        // Fixed amount as it does not depend on an exercise
        BigDecimal amount = new BigDecimal("50000.00");

        Cashbox inscriptionCashbox = cashboxRepository.findByName(CashboxName.INSCRIPTION)
                .orElseThrow(() -> new BusinessException("Caisse d'inscription introuvable"));

        // Update cashbox balance
        inscriptionCashbox.setBalance(inscriptionCashbox.getBalance().add(amount));
        cashboxRepository.save(inscriptionCashbox);

        // Create Payment record
        Payment payment = Payment.builder()
                .member(savedMember)
                .cashbox(inscriptionCashbox)
                .administrator(administrator)
                .paymentType(PaymentType.INSCRIPTION)
                .amount(amount)
                .paymentDate(LocalDateTime.now())
                .status("COMPLETED")
                .build();
        paymentRepository.save(payment);

        // Record in Transaction Log
        TransactionLog log = TransactionLog.builder()
                .transactionDate(LocalDateTime.now())
                .member(savedMember)
                .cashbox(inscriptionCashbox)
                .type(TransactionType.INFLOW)
                .category("INSCRIPTION_PAYMENT")
                .amount(amount)
                .referenceTable("member")
                .referenceId(savedMember.getId())
                .description("Automatic registration fee payment for " + savedMember.getUsername())
                .build();
        transactionLogRepository.save(log);

        return savedMember;
    }

    public List<Member> getAllMembers() {
        List<Member> members = memberRepository.findAll();
        
        // Ensure all administrators also have a Member profile
        List<Administrator> admins = adminRepository.findAll();
        boolean renewed = false;
        for (Administrator admin : admins) {
            if (memberRepository.findByUser(admin.getUser()).isEmpty()) {
                Member virtualMember = Member.builder()
                        .user(admin.getUser())
                        .administrator(admin)
                        .username(admin.getUsername())
                        .inscriptionDate(java.time.LocalDate.now())
                        .active(true)
                        .build();
                memberRepository.save(virtualMember);
                renewed = true;
            }
        }
        
        if (renewed) {
            return memberRepository.findAll();
        }
        return members;
    }

    public Member getMemberById(Long id) {
        return memberRepository.findById(id).orElseThrow(() -> new BusinessException("Membre introuvable"));
    }

    public Member getMemberByEmail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new BusinessException("Utilisateur introuvable"));
        return memberRepository.findByUser(user).orElseThrow(() -> new BusinessException("Profil membre introuvable"));
    }

    @Transactional
    public Member updateMember(Long id, RegisterMemberRequest request) {
        Member member = getMemberById(id);
        User user = member.getUser();
        user.setName(request.getName());
        user.setFirstName(request.getFirstName());
        user.setTel(request.getTel());
        user.setAddress(request.getAddress());
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        member.setUsername(request.getUsername());
        return memberRepository.save(member);
    }

    @Transactional
    public void deactivateMember(Long id) {
        Member member = getMemberById(id);
        member.setActive(false);
        memberRepository.save(member);
    }

    @Transactional
    public void activateMember(Long id) {
        Member member = getMemberById(id);
        member.setActive(true);
        memberRepository.save(member);
    }

    public String getMemberStatus(Long id) {
        Member member = getMemberById(id);
        
        BigDecimal sDebt = getSolidarityDebtAmount(id);
        BigDecimal rDebt = getRefuelingDebtAmount(id);
        BigDecimal bDebt = getBorrowingDebtAmount(id);
        
        BigDecimal totalFinancialDebt = sDebt.add(rDebt).add(bDebt);
        
        boolean hasPaidSolidarity = sDebt.compareTo(BigDecimal.ZERO) <= 0;
        boolean hasPaidRefueling = rDebt.compareTo(BigDecimal.ZERO) <= 0;
        
        if (hasPaidSolidarity && hasPaidRefueling) {
            return "EN_REGLE";
        } else {
            // Un membre non en règle est soit Insolvable, soit Inactif selon sa dette totale
            if (totalFinancialDebt.compareTo(new BigDecimal("250000")) < 0) {
                return "INSOLVABLE";
            } else {
                return "INACTIF";
            }
        }
    }

    private BigDecimal getSolidarityDebtAmount(Long memberId) {
        return solidarityDebtRepository.findByMemberId(memberId)
                .map(SolidarityDebt::getRemainingDebt)
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal getRefuelingDebtAmount(Long memberId) {
        // Total dû pour le renflouement
        BigDecimal totalDue = refuelingDistributionRepository.findByMemberId(memberId).stream()
                .map(RefuelingDistribution::getAmountReceived) // amountReceived est utilisé ici comme montant à renflouer
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Total payé pour le renflouement
        BigDecimal totalPaid = paymentRepository.findByMemberIdAndPaymentType(memberId, com.mutuelle.enums.PaymentType.REFUELING).stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return totalDue.subtract(totalPaid).max(BigDecimal.ZERO);
    }

    private BigDecimal getBorrowingDebtAmount(Long memberId) {
        return borrowingRepository.findByMemberId(memberId).stream()
                .filter(b -> b.getStatus() != com.mutuelle.enums.BorrowingStatus.COMPLETED)
                .map(Borrowing::getRemainingBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public java.util.List<java.util.Map<String, Object>> getMemberDebts(Long id) {
        java.util.List<java.util.Map<String, Object>> list = new java.util.ArrayList<>();
        
        // 1. Solidarity Debt
        solidarityDebtRepository.findByMemberId(id).ifPresent(sDebt -> {
            if (sDebt.getRemainingDebt().compareTo(java.math.BigDecimal.ZERO) > 0) {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", "solidarity-" + sDebt.getId());
                map.put("type", "SOLIDARITY");
                map.put("label", "Dette Solidarité");
                map.put("amount", sDebt.getRemainingDebt());
                list.add(map);
            }
        });

        // 2. Loans (Borrowings)
        borrowingRepository.findByMemberId(id).forEach(loan -> {
            if (loan.getStatus() != com.mutuelle.enums.BorrowingStatus.COMPLETED && 
                loan.getRemainingBalance().compareTo(java.math.BigDecimal.ZERO) > 0) {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", "loan-" + loan.getId());
                map.put("type", "LOAN");
                map.put("label", "Remboursement Prêt #" + loan.getId());
                map.put("amount", loan.getRemainingBalance());
                list.add(map);
            }
        });

        return list;
    }

    @Transactional
    public void deleteMember(Long id) {
        Member member = getMemberById(id);
        User user = member.getUser();
        memberRepository.delete(member);
        userRepository.delete(user);
    }

    @Transactional
    public Member updateProfile(Long id, String name, String firstName, String username, String tel, String address) {
        Member member = getMemberById(id);
        User user = member.getUser();
        user.setName(name);
        user.setFirstName(firstName);
        user.setTel(tel);
        user.setAddress(address);
        member.setUsername(username);
        userRepository.save(user);
        return memberRepository.save(member);
    }

    @Transactional
    public Member updateAvatar(Long id, String avatarUrl) {
        Member member = getMemberById(id);
        User user = member.getUser();
        user.setAvatar(avatarUrl);
        userRepository.save(user);
        return memberRepository.save(member);
    }
}
