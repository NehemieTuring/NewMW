package com.mutuelle.service;

import com.mutuelle.dto.request.RegisterMemberRequest;
import com.mutuelle.entity.*;
import com.mutuelle.enums.BorrowingStatus;
import com.mutuelle.enums.CashboxName;
import com.mutuelle.enums.RoleType;
import com.mutuelle.exception.BusinessException;
import com.mutuelle.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService — Tests Unitaires")
class MemberServiceTest {

    @Mock private MemberRepository memberRepository;
    @Mock private UserRepository userRepository;
    @Mock private AdministratorRepository adminRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private SolidarityDebtRepository solidarityDebtRepository;
    @Mock private BorrowingRepository borrowingRepository;
    @Mock private CashboxRepository cashboxRepository;
    @Mock private TransactionLogRepository transactionLogRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private ExerciseService exerciseService;
    @Mock private RefuelingDistributionRepository refuelingDistributionRepository;

    @InjectMocks
    private MemberService memberService;

    private Administrator admin;

    @BeforeEach
    void setUp() {
        User adminUser = User.builder().id(10L).name("Admin").firstName("Super").email("admin@test.com").build();
        admin = Administrator.builder().id(1L).user(adminUser).username("superadmin").build();
    }

    // ========================================================================
    // Inscription d'un Membre
    // ========================================================================
    @Nested
    @DisplayName("Inscription (register)")
    class RegisterTests {

        @Test
        @DisplayName("Inscription refusée si email déjà utilisé")
        void shouldRejectDuplicateEmail() {
            RegisterMemberRequest request = new RegisterMemberRequest();
            request.setEmail("existing@test.com");
            when(userRepository.findByEmail("existing@test.com")).thenReturn(Optional.of(new User()));

            assertThatThrownBy(() -> memberService.register(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("email est déjà utilisé");
        }

        @Test
        @DisplayName("Inscription refusée si nom d'utilisateur déjà pris")
        void shouldRejectDuplicateUsername() {
            RegisterMemberRequest request = new RegisterMemberRequest();
            request.setEmail("new@test.com");
            request.setUsername("existingUser");
            request.setPassword("password");
            when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
            when(memberRepository.findByUsername("existingUser")).thenReturn(Optional.of(new Member()));

            assertThatThrownBy(() -> memberService.register(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("nom d'utilisateur existe déjà");
        }

        @Test
        @DisplayName("Inscription réussie crée User + Member + SolidarityDebt + Payment")
        void shouldRegisterSuccessfully() {
            RegisterMemberRequest request = new RegisterMemberRequest();
            request.setName("Dupont");
            request.setFirstName("Marie");
            request.setEmail("marie@test.com");
            request.setUsername("mariedupont");
            request.setPassword("secret123");
            request.setAdminId(1L);
            request.setTel("690000000");
            request.setAddress("Yaoundé");

            when(userRepository.findByEmail("marie@test.com")).thenReturn(Optional.empty());
            when(memberRepository.findByUsername("mariedupont")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("secret123")).thenReturn("$encoded$");
            when(userRepository.save(any(User.class))).thenAnswer(i -> {
                User u = i.getArgument(0);
                u.setId(2L);
                return u;
            });
            when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));
            when(memberRepository.save(any(Member.class))).thenAnswer(i -> {
                Member m = i.getArgument(0);
                m.setId(2L);
                return m;
            });
            
            Cashbox inscriptionCashbox = Cashbox.builder().id(2L).name(CashboxName.INSCRIPTION)
                    .balance(new BigDecimal("500000")).build();
            when(cashboxRepository.findByName(CashboxName.INSCRIPTION)).thenReturn(Optional.of(inscriptionCashbox));
            when(solidarityDebtRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(transactionLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Member result = memberService.register(request);

            // Vérifications
            assertThat(result.getId()).isEqualTo(2L);
            assertThat(result.isActive()).isTrue();

            // User créé avec le bon type
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getType()).isEqualTo(RoleType.MEMBER);
            assertThat(userCaptor.getValue().getPassword()).isEqualTo("$encoded$");

            // SolidarityDebt initialisée à zéro
            ArgumentCaptor<SolidarityDebt> debtCaptor = ArgumentCaptor.forClass(SolidarityDebt.class);
            verify(solidarityDebtRepository).save(debtCaptor.capture());
            assertThat(debtCaptor.getValue().getTotalDue()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(debtCaptor.getValue().getRemainingDebt()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(debtCaptor.getValue().getStatus()).isEqualTo("UP_TO_DATE");

            // Frais d'inscription (50 000 XAF) dans la caisse inscription
            assertThat(inscriptionCashbox.getBalance()).isEqualByComparingTo(new BigDecimal("550000"));

            // Payment créé
            verify(paymentRepository).save(any(Payment.class));
            // TransactionLog créé
            verify(transactionLogRepository).save(any(TransactionLog.class));
        }
    }

    // ========================================================================
    // Statut du Membre — Règle de Conformité
    // ========================================================================
    @Nested
    @DisplayName("Statut du membre (getMemberStatus)")
    class GetMemberStatusTests {

        private Member testMember;

        @BeforeEach
        void setup() {
            testMember = Member.builder().id(5L).active(true).build();
        }

        @Test
        @DisplayName("Membre EN_REGLE si solidarité et renflouement payés")
        void shouldReturnEnRegle() {
            when(memberRepository.findById(5L)).thenReturn(Optional.of(testMember));
            // Solidarité : dette = 0
            when(solidarityDebtRepository.findByMemberId(5L))
                    .thenReturn(Optional.of(SolidarityDebt.builder()
                            .remainingDebt(BigDecimal.ZERO).build()));
            // Renflouement : pas de distribution
            when(refuelingDistributionRepository.findByMemberId(5L)).thenReturn(Collections.emptyList());
            // Emprunts : aucun actif
            when(borrowingRepository.findByMemberId(5L)).thenReturn(Collections.emptyList());
            // Payments renflouement : 0
            when(paymentRepository.findByMemberIdAndPaymentType(eq(5L), eq(com.mutuelle.enums.PaymentType.REFUELING)))
                    .thenReturn(Collections.emptyList());

            String status = memberService.getMemberStatus(5L);

            assertThat(status).isEqualTo("EN_REGLE");
        }

        @Test
        @DisplayName("Membre INSOLVABLE si dette totale < 250 000 XAF")
        void shouldReturnInsolvable() {
            when(memberRepository.findById(5L)).thenReturn(Optional.of(testMember));
            // Solidarité : dette = 100 000
            when(solidarityDebtRepository.findByMemberId(5L))
                    .thenReturn(Optional.of(SolidarityDebt.builder()
                            .remainingDebt(new BigDecimal("100000")).build()));
            // Renflouement : 0
            when(refuelingDistributionRepository.findByMemberId(5L)).thenReturn(Collections.emptyList());
            when(paymentRepository.findByMemberIdAndPaymentType(eq(5L), eq(com.mutuelle.enums.PaymentType.REFUELING)))
                    .thenReturn(Collections.emptyList());
            // Emprunts : 100 000 restant
            Borrowing activeLoan = Borrowing.builder().status(BorrowingStatus.ACTIVE)
                    .remainingBalance(new BigDecimal("100000")).build();
            when(borrowingRepository.findByMemberId(5L)).thenReturn(List.of(activeLoan));

            String status = memberService.getMemberStatus(5L);

            // Total = 100 000 (solidarité) + 100 000 (emprunt) = 200 000 < 250 000
            assertThat(status).isEqualTo("INSOLVABLE");
        }

        @Test
        @DisplayName("Membre INACTIF si dette totale >= 250 000 XAF")
        void shouldReturnInactif() {
            when(memberRepository.findById(5L)).thenReturn(Optional.of(testMember));
            // Solidarité : dette = 150 000
            when(solidarityDebtRepository.findByMemberId(5L))
                    .thenReturn(Optional.of(SolidarityDebt.builder()
                            .remainingDebt(new BigDecimal("150000")).build()));
            // Renflouement : dû = 50 000
            RefuelingDistribution rd = RefuelingDistribution.builder().amountReceived(new BigDecimal("50000")).build();
            when(refuelingDistributionRepository.findByMemberId(5L)).thenReturn(List.of(rd));
            when(paymentRepository.findByMemberIdAndPaymentType(eq(5L), eq(com.mutuelle.enums.PaymentType.REFUELING)))
                    .thenReturn(Collections.emptyList());
            // Emprunts : 100 000 restant
            Borrowing activeLoan = Borrowing.builder().status(BorrowingStatus.ACTIVE)
                    .remainingBalance(new BigDecimal("100000")).build();
            when(borrowingRepository.findByMemberId(5L)).thenReturn(List.of(activeLoan));

            String status = memberService.getMemberStatus(5L);

            // Total = 150 000 (solidarité) + 50 000 (renflouement) + 100 000 (emprunt) = 300 000 >= 250 000
            assertThat(status).isEqualTo("INACTIF");
        }
    }

    // ========================================================================
    // Dettes du Membre
    // ========================================================================
    @Nested
    @DisplayName("Liste des dettes (getMemberDebts)")
    class GetMemberDebtsTests {

        @Test
        @DisplayName("Retourne les dettes de solidarité et emprunts actifs")
        void shouldReturnAllActiveDebts() {
            // Solidarité
            SolidarityDebt sDebt = SolidarityDebt.builder().id(1L)
                    .remainingDebt(new BigDecimal("75000")).build();
            when(solidarityDebtRepository.findByMemberId(5L)).thenReturn(Optional.of(sDebt));
            // Emprunts
            Borrowing loan1 = Borrowing.builder().id(10L).status(BorrowingStatus.ACTIVE)
                    .remainingBalance(new BigDecimal("300000")).build();
            Borrowing loan2 = Borrowing.builder().id(11L).status(BorrowingStatus.COMPLETED)
                    .remainingBalance(BigDecimal.ZERO).build();
            when(borrowingRepository.findByMemberId(5L)).thenReturn(List.of(loan1, loan2));

            var debts = memberService.getMemberDebts(5L);

            assertThat(debts).hasSize(2);
            // 1 solidarity + 1 active loan (completed excluded)
        }

        @Test
        @DisplayName("Aucune dette si tout est soldé")
        void shouldReturnEmptyIfNoDebts() {
            when(solidarityDebtRepository.findByMemberId(5L))
                    .thenReturn(Optional.of(SolidarityDebt.builder().remainingDebt(BigDecimal.ZERO).build()));
            when(borrowingRepository.findByMemberId(5L)).thenReturn(Collections.emptyList());

            var debts = memberService.getMemberDebts(5L);
            assertThat(debts).isEmpty();
        }
    }

    // ========================================================================
    // Activation / Désactivation
    // ========================================================================
    @Nested
    @DisplayName("Activation / Désactivation")
    class ActivationTests {

        @Test
        @DisplayName("Désactivation met active=false")
        void shouldDeactivateMember() {
            Member m = Member.builder().id(1L).active(true).build();
            when(memberRepository.findById(1L)).thenReturn(Optional.of(m));
            when(memberRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            memberService.deactivateMember(1L);
            assertThat(m.isActive()).isFalse();
        }

        @Test
        @DisplayName("Activation met active=true")
        void shouldActivateMember() {
            Member m = Member.builder().id(1L).active(false).build();
            when(memberRepository.findById(1L)).thenReturn(Optional.of(m));
            when(memberRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            memberService.activateMember(1L);
            assertThat(m.isActive()).isTrue();
        }
    }
}
