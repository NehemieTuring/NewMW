package com.mutuelle.service;

import com.mutuelle.entity.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import com.mutuelle.enums.BorrowingStatus;
import com.mutuelle.enums.CashboxName;
import com.mutuelle.enums.TransactionType;
import com.mutuelle.exception.BusinessException;
import com.mutuelle.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
@DisplayName("BorrowingService — Tests Unitaires")
class BorrowingServiceTest {

    @Mock private BorrowingRepository borrowingRepository;
    @Mock private MemberService memberService;
    @Mock private SessionService sessionService;
    @Mock private SavingService savingService;
    @Mock private CashboxRepository cashboxRepository;
    @Mock private TransactionLogRepository transactionLogRepository;
    @Mock private RefundRepository refundRepository;
    @Mock private Clock clock;

    @InjectMocks
    private BorrowingService borrowingService;

    private Member activeMember;
    private Administrator admin;
    private Session activeSession;
    private Exercise exercise;
    private Cashbox savingCashbox;

    @BeforeEach
    void setUp() {
        lenient().when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        lenient().when(clock.instant()).thenReturn(Instant.now());

        User user = User.builder().id(1L).name("Doe").firstName("John").email("john@test.com").build();
        admin = Administrator.builder().id(1L).user(user).username("admin1").build();
        activeMember = Member.builder().id(1L).user(user).administrator(admin)
                .username("johndoe").active(true).inscriptionDate(LocalDate.now()).build();
        
        exercise = Exercise.builder().id(1L).year("2026").interestRate(new BigDecimal("3.00"))
                .startDate(LocalDate.of(2026, 1, 1)).endDate(LocalDate.of(2026, 12, 31)).active(true).build();
        
        activeSession = Session.builder().id(1L).exercise(exercise).administrator(admin)
                .sessionNumber(1).date(LocalDate.now()).active(true).build();
        
        savingCashbox = Cashbox.builder().id(1L).name(CashboxName.SAVING)
                .balance(new BigDecimal("5000000")).build();
    }

    // ========================================================================
    // Paliers Dégressifs — calculateMaxLoan
    // ========================================================================
    @Nested
    @DisplayName("Calcul du plafond d'emprunt (paliers dégressifs)")
    class CalculateMaxLoanTests {

        @ParameterizedTest(name = "Épargne={0} → Multiplicateur x{1} → Plafond={2}")
        @CsvSource({
            "0,       5,   0",
            "100000,  5,   500000",
            "500000,  5,   2500000",
            "500001,  4,   2000004",
            "750000,  4,   3000000",
            "1000000, 4,   4000000",
            "1000001, 3,   3000003",
            "1250000, 3,   3750000",
            "1500000, 3,   4500000",
            "1500001, 2,   3000002",
            "1750000, 2,   3500000",
            "2000000, 2,   4000000",
            "2000001, 1.5, 3000002",
            "3000000, 1.5, 4500000",
            "5000000, 1.5, 7500000"
        })
        @DisplayName("Palier dégressif correct")
        void shouldCalculateMaxLoanAccordingToTier(String savings, String multiplier, String expectedMax) {
            BigDecimal result = borrowingService.calculateMaxLoan(new BigDecimal(savings));
            assertThat(result).isEqualByComparingTo(new BigDecimal(expectedMax));
        }

        @Test
        @DisplayName("Épargne nulle retourne zéro")
        void shouldReturnZeroForNullSavings() {
            assertThat(borrowingService.calculateMaxLoan(null)).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Épargne zéro retourne zéro")
        void shouldReturnZeroForZeroSavings() {
            assertThat(borrowingService.calculateMaxLoan(BigDecimal.ZERO)).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // ========================================================================
    // Demande de prêt — requestLoan
    // ========================================================================
    @Nested
    @DisplayName("Demande de prêt (requestLoan)")
    class RequestLoanTests {

        @Test
        @DisplayName("Prêt refusé si le membre est inactif")
        void shouldRejectLoanForInactiveMember() {
            Member inactiveMember = Member.builder().id(2L).active(false).build();
            when(memberService.getMemberById(2L)).thenReturn(inactiveMember);
            when(sessionService.getActiveSession()).thenReturn(activeSession);

            assertThatThrownBy(() -> borrowingService.requestLoan(2L, new BigDecimal("100000"), admin))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("n'est pas actif");
        }

        @Test
        @DisplayName("Prêt refusé si un prêt actif existe déjà")
        void shouldRejectLoanWhenActiveLoanExists() {
            when(memberService.getMemberById(1L)).thenReturn(activeMember);
            when(sessionService.getActiveSession()).thenReturn(activeSession);
            
            Borrowing existingLoan = Borrowing.builder().id(10L).status(BorrowingStatus.ACTIVE).build();
            when(borrowingRepository.findByMemberIdAndStatusIn(eq(1L), anyList()))
                    .thenReturn(List.of(existingLoan));

            assertThatThrownBy(() -> borrowingService.requestLoan(1L, new BigDecimal("100000"), admin))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("prêt en cours");
        }

        @Test
        @DisplayName("Prêt refusé si le montant dépasse le plafond dégressif")
        void shouldRejectLoanExceedingMaxAllowed() {
            when(memberService.getMemberById(1L)).thenReturn(activeMember);
            when(sessionService.getActiveSession()).thenReturn(activeSession);
            when(borrowingRepository.findByMemberIdAndStatusIn(eq(1L), anyList()))
                    .thenReturn(Collections.emptyList());
            // Épargne = 100 000 → plafond = 500 000
            when(savingService.getMemberBalance(1L)).thenReturn(new BigDecimal("100000"));

            assertThatThrownBy(() -> borrowingService.requestLoan(1L, new BigDecimal("600000"), admin))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("dépasse votre droit d'emprunt");
        }

        @Test
        @DisplayName("Prêt refusé si la caisse épargne est insuffisante")
        void shouldRejectLoanWhenCashboxInsufficientFunds() {
            when(memberService.getMemberById(1L)).thenReturn(activeMember);
            when(sessionService.getActiveSession()).thenReturn(activeSession);
            when(borrowingRepository.findByMemberIdAndStatusIn(eq(1L), anyList()))
                    .thenReturn(Collections.emptyList());
            when(savingService.getMemberBalance(1L)).thenReturn(new BigDecimal("500000")); // plafond = 2.5M

            Cashbox emptyCashbox = Cashbox.builder().id(1L).name(CashboxName.SAVING)
                    .balance(new BigDecimal("100")).build();
            when(cashboxRepository.findByName(CashboxName.SAVING)).thenReturn(Optional.of(emptyCashbox));
            when(borrowingRepository.save(any(Borrowing.class))).thenAnswer(i -> {
                Borrowing b = i.getArgument(0);
                b.setId(100L);
                return b;
            });

            assertThatThrownBy(() -> borrowingService.requestLoan(1L, new BigDecimal("1000000"), admin))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Fonds insuffisants");
        }

        @Test
        @DisplayName("Prêt accordé avec intérêts pré-déduits correctement (3%)")
        void shouldApproveLoanWithPreDeductedInterest() {
            when(memberService.getMemberById(1L)).thenReturn(activeMember);
            when(sessionService.getActiveSession()).thenReturn(activeSession);
            when(borrowingRepository.findByMemberIdAndStatusIn(eq(1L), anyList()))
                    .thenReturn(Collections.emptyList());
            when(savingService.getMemberBalance(1L)).thenReturn(new BigDecimal("500000")); // plafond = 2.5M
            when(cashboxRepository.findByName(CashboxName.SAVING)).thenReturn(Optional.of(savingCashbox));
            when(borrowingRepository.save(any(Borrowing.class))).thenAnswer(i -> {
                Borrowing b = i.getArgument(0);
                b.setId(100L);
                return b;
            });
            when(transactionLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            BigDecimal requestedAmount = new BigDecimal("1000000");
            Borrowing result = borrowingService.requestLoan(1L, requestedAmount, admin);

            // Vérifier les intérêts : 1 000 000 × 3% = 30 000
            assertThat(result.getInterestAmount()).isEqualByComparingTo(new BigDecimal("30000"));
            // Net reçu : 1 000 000 - 30 000 = 970 000
            assertThat(result.getNetAmountReceived()).isEqualByComparingTo(new BigDecimal("970000"));
            // Reste à rembourser = montant brut = 1 000 000
            assertThat(result.getRemainingBalance()).isEqualByComparingTo(requestedAmount);
            // Statut = ACTIVE
            assertThat(result.getStatus()).isEqualTo(BorrowingStatus.ACTIVE);
            // Échéance = +3 mois
            assertThat(result.getDueDate()).isEqualTo(LocalDate.now().plusMonths(3));
        }

        @Test
        @DisplayName("Le prêt décrémente la caisse Épargne du montant NET")
        void shouldDecrementSavingCashboxByNetAmount() {
            when(memberService.getMemberById(1L)).thenReturn(activeMember);
            when(sessionService.getActiveSession()).thenReturn(activeSession);
            when(borrowingRepository.findByMemberIdAndStatusIn(eq(1L), anyList()))
                    .thenReturn(Collections.emptyList());
            when(savingService.getMemberBalance(1L)).thenReturn(new BigDecimal("500000"));
            when(cashboxRepository.findByName(CashboxName.SAVING)).thenReturn(Optional.of(savingCashbox));
            when(borrowingRepository.save(any(Borrowing.class))).thenAnswer(i -> {
                Borrowing b = i.getArgument(0);
                b.setId(100L);
                return b;
            });
            when(transactionLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            BigDecimal initialBalance = savingCashbox.getBalance();
            borrowingService.requestLoan(1L, new BigDecimal("1000000"), admin);

            // Net = 970 000, donc caisse = 5 000 000 - 970 000 = 4 030 000
            assertThat(savingCashbox.getBalance()).isEqualByComparingTo(initialBalance.subtract(new BigDecimal("970000")));
        }

        @Test
        @DisplayName("Un TransactionLog OUTFLOW est créé lors du prêt")
        void shouldCreateOutflowTransactionLog() {
            when(memberService.getMemberById(1L)).thenReturn(activeMember);
            when(sessionService.getActiveSession()).thenReturn(activeSession);
            when(borrowingRepository.findByMemberIdAndStatusIn(eq(1L), anyList()))
                    .thenReturn(Collections.emptyList());
            when(savingService.getMemberBalance(1L)).thenReturn(new BigDecimal("500000"));
            when(cashboxRepository.findByName(CashboxName.SAVING)).thenReturn(Optional.of(savingCashbox));
            when(borrowingRepository.save(any(Borrowing.class))).thenAnswer(i -> {
                Borrowing b = i.getArgument(0);
                b.setId(100L);
                return b;
            });
            when(transactionLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            borrowingService.requestLoan(1L, new BigDecimal("1000000"), admin);

            ArgumentCaptor<TransactionLog> captor = ArgumentCaptor.forClass(TransactionLog.class);
            verify(transactionLogRepository).save(captor.capture());
            TransactionLog log = captor.getValue();
            assertThat(log.getType()).isEqualTo(TransactionType.OUTFLOW);
            assertThat(log.getCategory()).isEqualTo("BORROWING_LOAN");
            assertThat(log.getAmount()).isEqualByComparingTo(new BigDecimal("970000"));
        }
    }

    // ========================================================================
    // Remboursement — recordRefund
    // ========================================================================
    @Nested
    @DisplayName("Remboursement de prêt (recordRefund)")
    class RecordRefundTests {

        private Borrowing activeBorrowing;

        @BeforeEach
        void setUpBorrowing() {
            activeBorrowing = Borrowing.builder()
                    .id(10L).member(activeMember).session(activeSession)
                    .requestedAmount(new BigDecimal("1000000"))
                    .approvedAmount(new BigDecimal("1000000"))
                    .interestAmount(new BigDecimal("30000"))
                    .netAmountReceived(new BigDecimal("970000"))
                    .remainingBalance(new BigDecimal("1000000"))
                    .status(BorrowingStatus.ACTIVE)
                    .dueDate(LocalDate.now().plusMonths(3))
                    .build();
        }

        @Test
        @DisplayName("Remboursement refusé si le prêt est déjà soldé")
        void shouldRejectRefundForCompletedLoan() {
            activeBorrowing.setStatus(BorrowingStatus.COMPLETED);
            when(borrowingRepository.findById(10L)).thenReturn(Optional.of(activeBorrowing));

            assertThatThrownBy(() -> borrowingService.recordRefund(10L, new BigDecimal("50000"), admin))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("soldé");
        }

        @Test
        @DisplayName("Remboursement refusé si le montant est négatif ou zéro")
        void shouldRejectRefundWithZeroOrNegativeAmount() {
            when(borrowingRepository.findById(10L)).thenReturn(Optional.of(activeBorrowing));

            assertThatThrownBy(() -> borrowingService.recordRefund(10L, BigDecimal.ZERO, admin))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("supérieur à zéro");

            assertThatThrownBy(() -> borrowingService.recordRefund(10L, new BigDecimal("-100"), admin))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("supérieur à zéro");
        }

        @Test
        @DisplayName("Remboursement refusé si le montant dépasse le reste à payer")
        void shouldRejectRefundExceedingRemainingBalance() {
            when(borrowingRepository.findById(10L)).thenReturn(Optional.of(activeBorrowing));

            assertThatThrownBy(() -> borrowingService.recordRefund(10L, new BigDecimal("1500000"), admin))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("dépasse le reste à payer");
        }

        @Test
        @DisplayName("Remboursement partiel diminue le solde restant")
        void shouldReduceRemainingBalanceOnPartialRefund() {
            when(borrowingRepository.findById(10L)).thenReturn(Optional.of(activeBorrowing));
            when(sessionService.getActiveSession()).thenReturn(activeSession);
            when(cashboxRepository.findByName(CashboxName.SAVING)).thenReturn(Optional.of(savingCashbox));
            when(refundRepository.save(any(Refund.class))).thenAnswer(i -> {
                Refund r = i.getArgument(0);
                r.setId(1L);
                return r;
            });
            when(transactionLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Refund refund = borrowingService.recordRefund(10L, new BigDecimal("400000"), admin);

            assertThat(activeBorrowing.getRemainingBalance()).isEqualByComparingTo(new BigDecimal("600000"));
            assertThat(activeBorrowing.getStatus()).isEqualTo(BorrowingStatus.ACTIVE);
            assertThat(refund.getAmount()).isEqualByComparingTo(new BigDecimal("400000"));
        }

        @Test
        @DisplayName("Remboursement total marque le prêt comme COMPLETED")
        void shouldMarkLoanAsCompletedOnFullRefund() {
            when(borrowingRepository.findById(10L)).thenReturn(Optional.of(activeBorrowing));
            when(sessionService.getActiveSession()).thenReturn(activeSession);
            when(cashboxRepository.findByName(CashboxName.SAVING)).thenReturn(Optional.of(savingCashbox));
            when(refundRepository.save(any(Refund.class))).thenAnswer(i -> {
                Refund r = i.getArgument(0);
                r.setId(1L);
                return r;
            });
            when(transactionLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            borrowingService.recordRefund(10L, new BigDecimal("1000000"), admin);

            assertThat(activeBorrowing.getRemainingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(activeBorrowing.getStatus()).isEqualTo(BorrowingStatus.COMPLETED);
        }

        @Test
        @DisplayName("Le remboursement recrédite la caisse Épargne")
        void shouldCreditSavingCashboxOnRefund() {
            when(borrowingRepository.findById(10L)).thenReturn(Optional.of(activeBorrowing));
            when(sessionService.getActiveSession()).thenReturn(activeSession);
            when(cashboxRepository.findByName(CashboxName.SAVING)).thenReturn(Optional.of(savingCashbox));
            when(refundRepository.save(any(Refund.class))).thenAnswer(i -> {
                Refund r = i.getArgument(0);
                r.setId(1L);
                return r;
            });
            when(transactionLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            BigDecimal initialBalance = savingCashbox.getBalance();
            borrowingService.recordRefund(10L, new BigDecimal("300000"), admin);

            assertThat(savingCashbox.getBalance()).isEqualByComparingTo(initialBalance.add(new BigDecimal("300000")));
        }

        @Test
        @DisplayName("Un TransactionLog INFLOW est créé lors du remboursement")
        void shouldCreateInflowTransactionLog() {
            when(borrowingRepository.findById(10L)).thenReturn(Optional.of(activeBorrowing));
            when(sessionService.getActiveSession()).thenReturn(activeSession);
            when(cashboxRepository.findByName(CashboxName.SAVING)).thenReturn(Optional.of(savingCashbox));
            when(refundRepository.save(any(Refund.class))).thenAnswer(i -> {
                Refund r = i.getArgument(0);
                r.setId(1L);
                return r;
            });
            when(transactionLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            borrowingService.recordRefund(10L, new BigDecimal("300000"), admin);

            ArgumentCaptor<TransactionLog> captor = ArgumentCaptor.forClass(TransactionLog.class);
            verify(transactionLogRepository).save(captor.capture());
            assertThat(captor.getValue().getType()).isEqualTo(TransactionType.INFLOW);
            assertThat(captor.getValue().getCategory()).isEqualTo("LOAN_REFUND");
        }
    }
}
