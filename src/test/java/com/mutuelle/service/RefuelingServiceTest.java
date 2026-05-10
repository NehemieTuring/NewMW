package com.mutuelle.service;

import com.mutuelle.entity.*;
import com.mutuelle.enums.CashboxName;
import com.mutuelle.enums.TransactionType;
import com.mutuelle.exception.BusinessException;
import com.mutuelle.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.mockito.ArgumentCaptor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefuelingService — Tests Unitaires")
class RefuelingServiceTest {

    @Mock private RefuelingRepository refuelingRepository;
    @Mock private RefuelingDistributionRepository distributionRepository;
    @Mock private MemberService memberService;
    @Mock private ExerciseService exerciseService;
    @Mock private CashboxRepository cashboxRepository;
    @Mock private TransactionLogRepository transactionLogRepository;
    @Mock private SolidarityDebtRepository solidarityDebtRepository;

    @InjectMocks
    private RefuelingService refuelingService;

    private Administrator admin;
    private Exercise exercise;
    private Cashbox solidarityBox;

    @BeforeEach
    void setUp() {
        admin = Administrator.builder().id(1L).username("admin").build();
        exercise = Exercise.builder().id(1L).year("2026").build();
        solidarityBox = Cashbox.builder().id(10L).name(CashboxName.SOLIDARITY).build();
    }

    @Test
    @DisplayName("Calcul du renflouement : Distribution temporelle proportionnelle")
    void shouldCalculateRefuelingWithTemporalDistribution() {
        exercise.setStartDate(LocalDate.of(2026, 1, 1));
        exercise.setEndDate(LocalDate.of(2026, 12, 31));
        
        when(exerciseService.getAllExercises()).thenReturn(List.of(exercise));
        when(refuelingRepository.findByExerciseId(1L)).thenReturn(Optional.empty());
        when(cashboxRepository.findByName(CashboxName.SOLIDARITY)).thenReturn(Optional.of(solidarityBox));

        // Dépense 1 : 500 000 en Mars (10 membres présents)
        TransactionLog log1 = TransactionLog.builder()
                .amount(new BigDecimal("500000"))
                .type(TransactionType.OUTFLOW)
                .category("SOLIDARITY_EXPENDITURE")
                .transactionDate(LocalDateTime.of(2026, 3, 15, 10, 0))
                .build();
        
        // Dépense 2 : 400 000 en Septembre (11 membres présents)
        TransactionLog log2 = TransactionLog.builder()
                .amount(new BigDecimal("400000"))
                .type(TransactionType.OUTFLOW)
                .category("SOLIDARITY_EXPENDITURE")
                .transactionDate(LocalDateTime.of(2026, 9, 20, 10, 0))
                .build();
        
        when(transactionLogRepository.findByCashboxId(anyLong())).thenReturn(List.of(log1, log2));

        // 10 membres anciens (inscrits avant Mars)
        java.util.List<Member> members = new java.util.ArrayList<>();
        for (long i = 1; i <= 10; i++) {
            members.add(Member.builder().id(i).inscriptionDate(LocalDate.of(2025, 12, 31)).build());
        }
        // 1 membre nouveau (inscrit en Juillet)
        Member newMember = Member.builder().id(11L).inscriptionDate(LocalDate.of(2026, 7, 1)).build();
        members.add(newMember);
        
        when(memberService.getAllMembers()).thenReturn(members);
        when(refuelingRepository.save(any(Refueling.class))).thenAnswer(i -> i.getArgument(0));
        when(solidarityDebtRepository.findByMemberId(anyLong())).thenReturn(Optional.of(new SolidarityDebt()));

        refuelingService.calculateRefueling(1L, admin);

        // Vérification des dettes calculées :
        // Log 1 (500k) / 10 membres = 50 000 chacun
        // Log 2 (400k) / 11 membres = 36 363.636... chacun
        // Membre ancien total = 50 000 + 36 363.64 = 86 363.64
        // Membre nouveau total = 36 363.64
        
        ArgumentCaptor<RefuelingDistribution> distCaptor = ArgumentCaptor.forClass(RefuelingDistribution.class);
        verify(distributionRepository, times(11)).save(distCaptor.capture());
        
        List<RefuelingDistribution> dists = distCaptor.getAllValues();
        
        // Membre 1 (Ancien)
        RefuelingDistribution distAncien = dists.stream().filter(d -> d.getMember().getId() == 1L).findFirst().get();
        assertThat(distAncien.getAmountReceived()).isEqualByComparingTo(new BigDecimal("86363.64"));
        
        // Membre 11 (Nouveau)
        RefuelingDistribution distNouveau = dists.stream().filter(d -> d.getMember().getId() == 11L).findFirst().get();
        assertThat(distNouveau.getAmountReceived()).isEqualByComparingTo(new BigDecimal("36363.64"));
    }

    @Test
    @DisplayName("Erreur si déjà calculé")
    void shouldThrowIfAlreadyCalculated() {
        when(exerciseService.getAllExercises()).thenReturn(List.of(exercise));
        when(refuelingRepository.findByExerciseId(1L)).thenReturn(Optional.of(new Refueling()));

        assertThatThrownBy(() -> refuelingService.calculateRefueling(1L, admin))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already calculated");
    }
}
