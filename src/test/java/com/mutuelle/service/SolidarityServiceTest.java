package com.mutuelle.service;

import com.mutuelle.entity.*;
import com.mutuelle.enums.CashboxName;
import com.mutuelle.enums.PaymentType;
import com.mutuelle.enums.TransactionType;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SolidarityService — Tests Unitaires")
class SolidarityServiceTest {

    @Mock private SolidarityRepository solidarityRepository;
    @Mock private SolidarityDebtRepository solidarityDebtRepository;
    @Mock private MemberService memberService;
    @Mock private CashboxRepository cashboxRepository;
    @Mock private TransactionLogRepository transactionLogRepository;
    @Mock private PaymentRepository paymentRepository;

    @InjectMocks
    private SolidarityService solidarityService;

    private Member member;
    private Administrator admin;
    private Cashbox solidarityCashbox;

    @BeforeEach
    void setUp() {
        User user = User.builder().id(1L).name("Doe").firstName("John").email("john@test.com").build();
        admin = Administrator.builder().id(1L).user(user).username("admin1").build();
        member = Member.builder().id(1L).user(user).administrator(admin)
                .username("johndoe").active(true).inscriptionDate(LocalDate.now()).build();
        
        solidarityCashbox = Cashbox.builder().id(2L).name(CashboxName.SOLIDARITY)
                .balance(new BigDecimal("500000")).build();
    }

    // ========================================================================
    // Paiement de solidarité
    // ========================================================================
    @Nested
    @DisplayName("Paiement de solidarité (paySolidarity)")
    class PaySolidarityTests {

        @Test
        @DisplayName("Paiement partiel met à jour la dette et marque LATE")
        void shouldUpdateDebtAsLateForPartialPayment() {
            SolidarityDebt existingDebt = SolidarityDebt.builder()
                    .id(1L).member(member)
                    .totalDue(new BigDecimal("150000"))
                    .totalPaid(BigDecimal.ZERO)
                    .remainingDebt(new BigDecimal("150000"))
                    .status("LATE")
                    .build();

            when(memberService.getMemberById(1L)).thenReturn(member);
            when(cashboxRepository.findByName(CashboxName.SOLIDARITY)).thenReturn(Optional.of(solidarityCashbox));
            when(solidarityDebtRepository.findByMemberId(1L)).thenReturn(Optional.of(existingDebt));
            when(solidarityRepository.save(any(Solidarity.class))).thenAnswer(i -> {
                Solidarity s = i.getArgument(0);
                s.setId(1L);
                return s;
            });
            when(solidarityDebtRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(transactionLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            solidarityService.paySolidarity(1L, new BigDecimal("50000"), admin);

            assertThat(existingDebt.getTotalPaid()).isEqualByComparingTo(new BigDecimal("50000"));
            assertThat(existingDebt.getRemainingDebt()).isEqualByComparingTo(new BigDecimal("100000"));
            assertThat(existingDebt.getStatus()).isEqualTo("LATE");
        }

        @Test
        @DisplayName("Paiement total solde la dette et marque UP_TO_DATE")
        void shouldMarkUpToDateOnFullPayment() {
            SolidarityDebt existingDebt = SolidarityDebt.builder()
                    .id(1L).member(member)
                    .totalDue(new BigDecimal("150000"))
                    .totalPaid(new BigDecimal("100000"))
                    .remainingDebt(new BigDecimal("50000"))
                    .status("LATE")
                    .build();

            when(memberService.getMemberById(1L)).thenReturn(member);
            when(cashboxRepository.findByName(CashboxName.SOLIDARITY)).thenReturn(Optional.of(solidarityCashbox));
            when(solidarityDebtRepository.findByMemberId(1L)).thenReturn(Optional.of(existingDebt));
            when(solidarityRepository.save(any(Solidarity.class))).thenAnswer(i -> {
                Solidarity s = i.getArgument(0);
                s.setId(1L);
                return s;
            });
            when(solidarityDebtRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(transactionLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            solidarityService.paySolidarity(1L, new BigDecimal("50000"), admin);

            assertThat(existingDebt.getRemainingDebt()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(existingDebt.getStatus()).isEqualTo("UP_TO_DATE");
        }

        @Test
        @DisplayName("Paiement crédite la caisse Solidarité")
        void shouldCreditSolidarityCashbox() {
            SolidarityDebt debt = SolidarityDebt.builder().id(1L).member(member)
                    .totalDue(new BigDecimal("150000")).totalPaid(BigDecimal.ZERO)
                    .remainingDebt(new BigDecimal("150000")).status("LATE").build();

            when(memberService.getMemberById(1L)).thenReturn(member);
            when(cashboxRepository.findByName(CashboxName.SOLIDARITY)).thenReturn(Optional.of(solidarityCashbox));
            when(solidarityDebtRepository.findByMemberId(1L)).thenReturn(Optional.of(debt));
            when(solidarityRepository.save(any(Solidarity.class))).thenAnswer(i -> {
                Solidarity s = i.getArgument(0);
                s.setId(1L);
                return s;
            });
            when(solidarityDebtRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(transactionLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            BigDecimal initialBalance = solidarityCashbox.getBalance();
            solidarityService.paySolidarity(1L, new BigDecimal("75000"), admin);

            assertThat(solidarityCashbox.getBalance()).isEqualByComparingTo(initialBalance.add(new BigDecimal("75000")));
        }

        @Test
        @DisplayName("Paiement crée un Payment SOLIDARITY et un TransactionLog INFLOW")
        void shouldCreatePaymentAndTransactionLog() {
            SolidarityDebt debt = SolidarityDebt.builder().id(1L).member(member)
                    .totalDue(new BigDecimal("150000")).totalPaid(BigDecimal.ZERO)
                    .remainingDebt(new BigDecimal("150000")).status("LATE").build();

            when(memberService.getMemberById(1L)).thenReturn(member);
            when(cashboxRepository.findByName(CashboxName.SOLIDARITY)).thenReturn(Optional.of(solidarityCashbox));
            when(solidarityDebtRepository.findByMemberId(1L)).thenReturn(Optional.of(debt));
            when(solidarityRepository.save(any(Solidarity.class))).thenAnswer(i -> {
                Solidarity s = i.getArgument(0);
                s.setId(1L);
                return s;
            });
            when(solidarityDebtRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(transactionLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            solidarityService.paySolidarity(1L, new BigDecimal("50000"), admin);

            ArgumentCaptor<Payment> payCaptor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(payCaptor.capture());
            assertThat(payCaptor.getValue().getPaymentType()).isEqualTo(PaymentType.SOLIDARITY);

            ArgumentCaptor<TransactionLog> logCaptor = ArgumentCaptor.forClass(TransactionLog.class);
            verify(transactionLogRepository).save(logCaptor.capture());
            assertThat(logCaptor.getValue().getType()).isEqualTo(TransactionType.INFLOW);
            assertThat(logCaptor.getValue().getCategory()).isEqualTo("SOLIDARITY_PAYMENT");
        }

        @Test
        @DisplayName("Crée une nouvelle dette si elle n'existe pas encore (nouveau membre)")
        void shouldCreateNewDebtIfNoneExists() {
            when(memberService.getMemberById(1L)).thenReturn(member);
            when(cashboxRepository.findByName(CashboxName.SOLIDARITY)).thenReturn(Optional.of(solidarityCashbox));
            when(solidarityDebtRepository.findByMemberId(1L)).thenReturn(Optional.empty());
            when(solidarityRepository.save(any(Solidarity.class))).thenAnswer(i -> {
                Solidarity s = i.getArgument(0);
                s.setId(1L);
                return s;
            });
            when(solidarityDebtRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(transactionLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            solidarityService.paySolidarity(1L, new BigDecimal("150000"), admin);

            ArgumentCaptor<SolidarityDebt> debtCaptor = ArgumentCaptor.forClass(SolidarityDebt.class);
            verify(solidarityDebtRepository).save(debtCaptor.capture());
            SolidarityDebt created = debtCaptor.getValue();
            assertThat(created.getTotalDue()).isEqualByComparingTo(new BigDecimal("150000"));
            assertThat(created.getTotalPaid()).isEqualByComparingTo(new BigDecimal("150000"));
            assertThat(created.getRemainingDebt()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(created.getStatus()).isEqualTo("UP_TO_DATE");
        }
    }

    // ========================================================================
    // Consultation de dette
    // ========================================================================
    @Nested
    @DisplayName("Consultation de dette (getMemberDebt)")
    class GetMemberDebtTests {

        @Test
        @DisplayName("Retourne la dette existante du membre")
        void shouldReturnExistingDebt() {
            SolidarityDebt debt = SolidarityDebt.builder().id(1L)
                    .totalDue(new BigDecimal("150000"))
                    .totalPaid(new BigDecimal("75000"))
                    .remainingDebt(new BigDecimal("75000"))
                    .status("LATE").build();
            when(solidarityDebtRepository.findByMemberId(1L)).thenReturn(Optional.of(debt));

            SolidarityDebt result = solidarityService.getMemberDebt(1L);
            assertThat(result.getRemainingDebt()).isEqualByComparingTo(new BigDecimal("75000"));
        }

        @Test
        @DisplayName("Retourne une dette vide si aucune n'existe")
        void shouldReturnEmptyDebtIfNone() {
            when(solidarityDebtRepository.findByMemberId(99L)).thenReturn(Optional.empty());

            SolidarityDebt result = solidarityService.getMemberDebt(99L);
            assertThat(result.getRemainingDebt()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getStatus()).isEqualTo("UP_TO_DATE");
        }
    }
}
