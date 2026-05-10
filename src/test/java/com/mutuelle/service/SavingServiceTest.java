package com.mutuelle.service;

import com.mutuelle.entity.*;
import com.mutuelle.enums.CashboxName;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SavingService — Tests Unitaires")
class SavingServiceTest {

    @Mock private SavingRepository savingRepository;
    @Mock private MemberService memberService;
    @Mock private SessionService sessionService;
    @Mock private CashboxRepository cashboxRepository;
    @Mock private TransactionLogRepository transactionLogRepository;
    @Mock private PaymentRepository paymentRepository;

    @InjectMocks
    private SavingService savingService;

    private Member member;
    private Administrator admin;
    private Session activeSession;
    private Cashbox savingCashbox;

    @BeforeEach
    void setUp() {
        User user = User.builder().id(1L).name("Doe").firstName("John").email("john@test.com").build();
        admin = Administrator.builder().id(1L).user(user).username("admin1").build();
        member = Member.builder().id(1L).user(user).administrator(admin)
                .username("johndoe").active(true).inscriptionDate(LocalDate.now()).build();
        
        Exercise exercise = Exercise.builder().id(1L).year("2026").active(true).build();
        activeSession = Session.builder().id(1L).exercise(exercise).administrator(admin)
                .sessionNumber(1).date(LocalDate.now()).active(true).build();
        
        savingCashbox = Cashbox.builder().id(1L).name(CashboxName.SAVING)
                .balance(new BigDecimal("1000000")).build();
    }

    // ========================================================================
    // Dépôt d'épargne
    // ========================================================================
    @Nested
    @DisplayName("Dépôt d'épargne (deposit)")
    class DepositTests {

        @Test
        @DisplayName("Dépôt réussi met à jour le solde cumulatif")
        void shouldCalculateCumulativeTotal() {
            when(memberService.getMemberById(1L)).thenReturn(member);
            when(sessionService.getActiveSession()).thenReturn(activeSession);
            when(cashboxRepository.findByName(CashboxName.SAVING)).thenReturn(Optional.of(savingCashbox));
            
            // Savings existants : 200 000 + 100 000 = 300 000
            Saving existing1 = Saving.builder().amount(new BigDecimal("200000")).type(TransactionType.INFLOW).build();
            Saving existing2 = Saving.builder().amount(new BigDecimal("100000")).type(TransactionType.INFLOW).build();
            when(savingRepository.findByMemberId(1L)).thenReturn(List.of(existing1, existing2));
            when(savingRepository.save(any(Saving.class))).thenAnswer(i -> {
                Saving s = i.getArgument(0);
                s.setId(3L);
                return s;
            });
            when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(transactionLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Saving result = savingService.deposit(1L, new BigDecimal("150000"), admin);

            // Cumulatif = 200 000 + 100 000 + 150 000 = 450 000
            assertThat(result.getCumulativeTotal()).isEqualByComparingTo(new BigDecimal("450000"));
            assertThat(result.getType()).isEqualTo(TransactionType.INFLOW);
        }

        @Test
        @DisplayName("Dépôt crédite la caisse Épargne")
        void shouldCreditSavingCashbox() {
            when(memberService.getMemberById(1L)).thenReturn(member);
            when(sessionService.getActiveSession()).thenReturn(activeSession);
            when(cashboxRepository.findByName(CashboxName.SAVING)).thenReturn(Optional.of(savingCashbox));
            when(savingRepository.findByMemberId(1L)).thenReturn(Collections.emptyList());
            when(savingRepository.save(any(Saving.class))).thenAnswer(i -> {
                Saving s = i.getArgument(0);
                s.setId(1L);
                return s;
            });
            when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(transactionLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            BigDecimal initialBalance = savingCashbox.getBalance();
            savingService.deposit(1L, new BigDecimal("250000"), admin);

            assertThat(savingCashbox.getBalance()).isEqualByComparingTo(initialBalance.add(new BigDecimal("250000")));
        }

        @Test
        @DisplayName("Dépôt crée un Payment SAVING_DEPOSIT")
        void shouldCreatePaymentRecord() {
            when(memberService.getMemberById(1L)).thenReturn(member);
            when(sessionService.getActiveSession()).thenReturn(activeSession);
            when(cashboxRepository.findByName(CashboxName.SAVING)).thenReturn(Optional.of(savingCashbox));
            when(savingRepository.findByMemberId(1L)).thenReturn(Collections.emptyList());
            when(savingRepository.save(any(Saving.class))).thenAnswer(i -> {
                Saving s = i.getArgument(0);
                s.setId(1L);
                return s;
            });
            when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
            when(transactionLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            savingService.deposit(1L, new BigDecimal("100000"), admin);

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(captor.capture());
            assertThat(captor.getValue().getPaymentType()).isEqualTo(com.mutuelle.enums.PaymentType.SAVING_DEPOSIT);
            assertThat(captor.getValue().getAmount()).isEqualByComparingTo(new BigDecimal("100000"));
        }

        @Test
        @DisplayName("Le cumulatif tient compte des retraits (OUTFLOW)")
        void shouldAccountForWithdrawalsInCumulative() {
            when(memberService.getMemberById(1L)).thenReturn(member);
            when(sessionService.getActiveSession()).thenReturn(activeSession);
            when(cashboxRepository.findByName(CashboxName.SAVING)).thenReturn(Optional.of(savingCashbox));
            
            Saving deposit = Saving.builder().amount(new BigDecimal("500000")).type(TransactionType.INFLOW).build();
            Saving withdrawal = Saving.builder().amount(new BigDecimal("100000")).type(TransactionType.OUTFLOW).build();
            when(savingRepository.findByMemberId(1L)).thenReturn(List.of(deposit, withdrawal));
            when(savingRepository.save(any(Saving.class))).thenAnswer(i -> {
                Saving s = i.getArgument(0);
                s.setId(3L);
                return s;
            });
            when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(transactionLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Saving result = savingService.deposit(1L, new BigDecimal("200000"), admin);

            // Cumulatif = 500 000 - 100 000 + 200 000 = 600 000
            assertThat(result.getCumulativeTotal()).isEqualByComparingTo(new BigDecimal("600000"));
        }
    }

    // ========================================================================
    // Retrait d'épargne
    // ========================================================================
    @Nested
    @DisplayName("Retrait d'épargne (withdraw)")
    class WithdrawTests {

        @Test
        @DisplayName("Retrait refusé si solde insuffisant")
        void shouldRejectWithdrawalWhenInsufficientBalance() {
            // Balance = 100 000
            Saving s = Saving.builder().amount(new BigDecimal("100000")).type(TransactionType.INFLOW).build();
            when(savingRepository.findByMemberId(1L)).thenReturn(List.of(s));

            assertThatThrownBy(() -> savingService.withdraw(1L, new BigDecimal("200000"), admin))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Solde insuffisant");
        }

        @Test
        @DisplayName("Retrait réussi décrémente la caisse et enregistre OUTFLOW")
        void shouldSuccessfullyWithdraw() {
            Saving s = Saving.builder().amount(new BigDecimal("500000")).type(TransactionType.INFLOW).build();
            when(savingRepository.findByMemberId(1L)).thenReturn(List.of(s));
            when(memberService.getMemberById(1L)).thenReturn(member);
            when(sessionService.getActiveSession()).thenReturn(activeSession);
            when(cashboxRepository.findByName(CashboxName.SAVING)).thenReturn(Optional.of(savingCashbox));
            when(savingRepository.save(any(Saving.class))).thenAnswer(i -> {
                Saving sv = i.getArgument(0);
                sv.setId(2L);
                return sv;
            });

            BigDecimal initialBalance = savingCashbox.getBalance();
            Saving result = savingService.withdraw(1L, new BigDecimal("200000"), admin);

            assertThat(result.getType()).isEqualTo(TransactionType.OUTFLOW);
            assertThat(result.getCumulativeTotal()).isEqualByComparingTo(new BigDecimal("300000"));
            assertThat(savingCashbox.getBalance()).isEqualByComparingTo(initialBalance.subtract(new BigDecimal("200000")));
        }
    }

    // ========================================================================
    // Calcul du solde
    // ========================================================================
    @Nested
    @DisplayName("Calcul du solde membre (getMemberBalance)")
    class GetMemberBalanceTests {

        @Test
        @DisplayName("Solde correct avec dépôts et retraits mixtes")
        void shouldCalculateCorrectBalance() {
            List<Saving> savings = List.of(
                    Saving.builder().amount(new BigDecimal("300000")).type(TransactionType.INFLOW).build(),
                    Saving.builder().amount(new BigDecimal("100000")).type(TransactionType.OUTFLOW).build(),
                    Saving.builder().amount(new BigDecimal("200000")).type(TransactionType.INFLOW).build()
            );
            when(savingRepository.findByMemberId(1L)).thenReturn(savings);

            BigDecimal balance = savingService.getMemberBalance(1L);

            // 300 000 - 100 000 + 200 000 = 400 000
            assertThat(balance).isEqualByComparingTo(new BigDecimal("400000"));
        }

        @Test
        @DisplayName("Solde zéro si aucune épargne")
        void shouldReturnZeroForNoSavings() {
            when(savingRepository.findByMemberId(1L)).thenReturn(Collections.emptyList());

            assertThat(savingService.getMemberBalance(1L)).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}
