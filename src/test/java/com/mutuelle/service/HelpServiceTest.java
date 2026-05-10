package com.mutuelle.service;

import com.mutuelle.entity.*;
import com.mutuelle.enums.BorrowingStatus;
import com.mutuelle.enums.CashboxName;
import com.mutuelle.exception.BusinessException;
import com.mutuelle.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HelpService — Tests Unitaires")
class HelpServiceTest {

    @Mock private HelpRepository helpRepository;
    @Mock private HelpTypeRepository helpTypeRepository;
    @Mock private MemberService memberService;
    @Mock private ContributionRepository contributionRepository;
    @Mock private CashboxRepository cashboxRepository;
    @Mock private TransactionService transactionService;
    @Mock private BorrowingService borrowingService;
    @Mock private SolidarityService solidarityService;
    @Mock private com.mutuelle.repository.PenaltyRepository penaltyRepository;

    @InjectMocks
    private HelpService helpService;

    private Member beneficiary;
    private Administrator admin;
    private HelpType helpType;
    private Cashbox solidarityCashbox;

    @BeforeEach
    void setUp() {
        User user = User.builder().id(1L).name("Doe").firstName("Jane").email("jane@test.com").build();
        admin = Administrator.builder().id(1L).user(user).username("admin1").build();
        beneficiary = Member.builder().id(1L).user(user).administrator(admin)
                .username("janedoe").active(true).inscriptionDate(LocalDate.now()).build();
        helpType = HelpType.builder().id(1L).name("Décès").description("Aide décès")
                .defaultAmount(new BigDecimal("200000")).active(true).build();
        solidarityCashbox = Cashbox.builder().id(2L).name(CashboxName.SOLIDARITY)
                .balance(new BigDecimal("1000000")).build();
    }

    // ========================================================================
    // Création d'aide
    // ========================================================================
    @Nested
    @DisplayName("Création d'aide (createHelp)")
    class CreateHelpTests {

        @Test
        @DisplayName("Aide créée avec financement intégral depuis le Fonds Social")
        void shouldCreateHelpWithFullFunding() {
            BigDecimal targetAmount = new BigDecimal("200000");
            
            when(helpTypeRepository.findById(1L)).thenReturn(Optional.of(helpType));
            when(memberService.getMemberById(1L)).thenReturn(beneficiary);
            when(cashboxRepository.findByName(CashboxName.SOLIDARITY)).thenReturn(Optional.of(solidarityCashbox));
            when(helpRepository.save(any(Help.class))).thenAnswer(i -> {
                Help h = i.getArgument(0);
                h.setId(1L);
                return h;
            });
            when(contributionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            doNothing().when(transactionService).recordTransaction(any(), anyString(), anyString(), any(), any());

            Help result = helpService.createHelp(1L, 1L, targetAmount, admin);

            assertThat(result.getCollectedAmount()).isEqualByComparingTo(targetAmount);
            assertThat(result.getStatus()).isEqualTo("COMPLETED");
            verify(transactionService).recordTransaction(eq(targetAmount.negate()), eq("SOLIDARITY_HELP"), anyString(), eq(solidarityCashbox), isNull());
        }

        @Test
        @DisplayName("Aide refusée si le Fonds Social est insuffisant")
        void shouldRejectHelpIfFundInsufficient() {
            solidarityCashbox.setBalance(new BigDecimal("50000")); // Insuffisant

            when(helpTypeRepository.findById(1L)).thenReturn(Optional.of(helpType));
            when(memberService.getMemberById(1L)).thenReturn(beneficiary);
            when(cashboxRepository.findByName(CashboxName.SOLIDARITY)).thenReturn(Optional.of(solidarityCashbox));
            when(helpRepository.save(any(Help.class))).thenAnswer(i -> {
                Help h = i.getArgument(0);
                h.setId(1L);
                return h;
            });

            assertThatThrownBy(() -> helpService.createHelp(1L, 1L, new BigDecimal("200000"), admin))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("solde insuffisant");
        }

        @Test
        @DisplayName("Aide refusée si le type d'aide n'existe pas")
        void shouldRejectIfHelpTypeNotFound() {
            when(helpTypeRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> helpService.createHelp(99L, 1L, new BigDecimal("100000"), admin))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Type d'aide introuvable");
        }
    }

    // ========================================================================
    // Décaissement avec compensation de dettes
    // ========================================================================
    @Nested
    @DisplayName("Décaissement d'aide (disburseHelp)")
    class DisburseHelpTests {

        @Test
        @DisplayName("Décaissement rembourse d'abord la solidarité, puis les prêts")
        void shouldRepayDebtsBeforeDisbursing() {
            Help help = Help.builder().id(1L).member(beneficiary)
                    .collectedAmount(new BigDecimal("500000")).status("COMPLETED").build();

            when(helpRepository.findById(1L)).thenReturn(Optional.of(help));
            
            // Solidarity debt = 100 000
            SolidarityDebt sDebt = SolidarityDebt.builder()
                    .remainingDebt(new BigDecimal("100000")).build();
            when(solidarityService.getMemberDebt(1L)).thenReturn(sDebt);
            when(solidarityService.paySolidarity(anyLong(), any(), any())).thenReturn(new Solidarity());

            // Active loan = 200 000
            Borrowing loan = Borrowing.builder().id(10L).status(BorrowingStatus.ACTIVE)
                    .remainingBalance(new BigDecimal("200000")).build();
            when(borrowingService.getMemberLoans(1L)).thenReturn(List.of(loan));
            when(borrowingService.recordRefund(anyLong(), any(), any())).thenReturn(new Refund());

            helpService.disburseHelp(1L, admin);

            // Vérifie que la solidarité est payée en premier (100 000)
            verify(solidarityService).paySolidarity(eq(1L), eq(new BigDecimal("100000")), eq(admin));
            // Puis le prêt (200 000)
            verify(borrowingService).recordRefund(eq(10L), eq(new BigDecimal("200000")), eq(admin));
            // Statut DISBURSED
            assertThat(help.getStatus()).isEqualTo("DISBURSED");
        }

        @Test
        @DisplayName("Décaissement refusé si l'aide n'est ni ACTIVE ni COMPLETED")
        void shouldRejectDisbursementIfStatusInvalid() {
            Help help = Help.builder().id(1L).member(beneficiary).status("DISBURSED").build();
            when(helpRepository.findById(1L)).thenReturn(Optional.of(help));

            assertThatThrownBy(() -> helpService.disburseHelp(1L, admin))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("ne peut pas être décaissée");
        }

        @Test
        @DisplayName("Décaissement sans dettes ne touche rien")
        void shouldDisburseFulAmountIfNoDebts() {
            Help help = Help.builder().id(1L).member(beneficiary)
                    .collectedAmount(new BigDecimal("300000")).status("COMPLETED").build();

            when(helpRepository.findById(1L)).thenReturn(Optional.of(help));
            when(solidarityService.getMemberDebt(1L)).thenReturn(
                    SolidarityDebt.builder().remainingDebt(BigDecimal.ZERO).build());
            when(borrowingService.getMemberLoans(1L)).thenReturn(Collections.emptyList());

            helpService.disburseHelp(1L, admin);

            verify(solidarityService, never()).paySolidarity(anyLong(), any(), any());
            verify(borrowingService, never()).recordRefund(anyLong(), any(), any());
            assertThat(help.getStatus()).isEqualTo("DISBURSED");
        }
    }

    // ========================================================================
    // Contribution à une aide
    // ========================================================================
    @Nested
    @DisplayName("Contribution à une aide (contributeToHelp)")
    class ContributeTests {

        @Test
        @DisplayName("Contribution réussie met à jour le montant collecté")
        void shouldUpdateCollectedAmount() {
            Help help = Help.builder().id(1L).member(beneficiary)
                    .targetAmount(new BigDecimal("500000"))
                    .collectedAmount(new BigDecimal("200000"))
                    .status("ACTIVE").build();

            when(helpRepository.findById(1L)).thenReturn(Optional.of(help));
            when(memberService.getMemberById(1L)).thenReturn(beneficiary);
            when(contributionRepository.save(any())).thenAnswer(i -> {
                Contribution c = i.getArgument(0);
                c.setId(1L);
                return c;
            });
            when(helpRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Contribution result = helpService.contributeToHelp(1L, 1L, new BigDecimal("100000"));

            assertThat(help.getCollectedAmount()).isEqualByComparingTo(new BigDecimal("300000"));
            assertThat(help.getStatus()).isEqualTo("ACTIVE"); // Not yet at target
        }

        @Test
        @DisplayName("Contribution marque COMPLETED quand l'objectif est atteint")
        void shouldMarkCompletedWhenTargetReached() {
            Help help = Help.builder().id(1L).member(beneficiary)
                    .targetAmount(new BigDecimal("500000"))
                    .collectedAmount(new BigDecimal("400000"))
                    .status("ACTIVE").build();

            when(helpRepository.findById(1L)).thenReturn(Optional.of(help));
            when(memberService.getMemberById(1L)).thenReturn(beneficiary);
            when(contributionRepository.save(any())).thenAnswer(i -> {
                Contribution c = i.getArgument(0);
                c.setId(1L);
                return c;
            });
            when(helpRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            helpService.contributeToHelp(1L, 1L, new BigDecimal("100000"));

            assertThat(help.getStatus()).isEqualTo("COMPLETED");
        }

        @Test
        @DisplayName("Contribution refusée si l'aide n'est plus active")
        void shouldRejectContributionIfNotActive() {
            Help help = Help.builder().id(1L).status("COMPLETED").build();
            when(helpRepository.findById(1L)).thenReturn(Optional.of(help));
            when(memberService.getMemberById(1L)).thenReturn(beneficiary);

            assertThatThrownBy(() -> helpService.contributeToHelp(1L, 1L, new BigDecimal("50000")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("n'est plus active");
        }
    }

    // ========================================================================
    // Création de type d'aide
    // ========================================================================
    @Nested
    @DisplayName("Gestion des types d'aide")
    class HelpTypeTests {

        @Test
        @DisplayName("Créer un type d'aide")
        void shouldCreateHelpType() {
            when(helpTypeRepository.save(any(HelpType.class))).thenAnswer(i -> {
                HelpType ht = i.getArgument(0);
                ht.setId(2L);
                return ht;
            });

            HelpType result = helpService.createHelpType("Mariage", "Aide mariage", new BigDecimal("100000"));

            assertThat(result.getName()).isEqualTo("Mariage");
            assertThat(result.isActive()).isTrue();
        }
    }
}
