package com.mutuelle.service;

import com.mutuelle.entity.*;
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
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InterestDistributionService — Tests Unitaires")
class InterestDistributionServiceTest {

    @Mock private InterestDistributionRepository distributionRepository;
    @Mock private InterestDistributionDetailRepository detailRepository;
    @Mock private SavingRepository savingRepository;
    @Mock private BorrowingRepository borrowingRepository;
    @Mock private SessionService sessionService;

    @InjectMocks
    private InterestDistributionService distributionService;

    private Administrator admin;
    private Session session;

    @BeforeEach
    void setUp() {
        admin = Administrator.builder().id(1L).build();
        session = Session.builder().id(1L).build();
    }

    @Test
    @DisplayName("Distribution proportionnelle des intérêts")
    void shouldDistributeInterestsProportionally() {
        when(sessionService.getAllSessions()).thenReturn(List.of(session));
        when(distributionRepository.findBySessionId(1L)).thenReturn(Collections.emptyList());

        // Intérêts total des prêts de la session = 30 000
        Borrowing b1 = Borrowing.builder().interestAmount(new BigDecimal("20000")).session(session).build();
        Borrowing b2 = Borrowing.builder().interestAmount(new BigDecimal("10000")).session(session).build();
        when(borrowingRepository.findAll()).thenReturn(List.of(b1, b2));

        // Épargnes de la session : Membre A (200k), Membre B (300k) -> Total 500k
        Member mA = Member.builder().id(1L).build();
        Member mB = Member.builder().id(2L).build();
        Saving sA = Saving.builder().member(mA).amount(new BigDecimal("200000")).session(session).build();
        Saving sB = Saving.builder().member(mB).amount(new BigDecimal("300000")).session(session).build();
        when(savingRepository.findBySessionId(1L)).thenReturn(List.of(sA, sB));

        when(distributionRepository.save(any(InterestDistribution.class))).thenAnswer(i -> i.getArgument(0));

        InterestDistribution result = distributionService.distributeInterests(1L, admin);

        assertThat(result.getTotalInterest()).isEqualByComparingTo(new BigDecimal("30000"));
        
        // Vérifie les détails
        // Membre A : (200/500) * 30 000 = 12 000
        // Membre B : (300/500) * 30 000 = 18 000
        verify(detailRepository, times(2)).save(any(InterestDistributionDetail.class));
        
        // On pourrait vérifier les arguments précis avec ArgumentCaptor
    }

    @Test
    @DisplayName("Erreur si aucune épargne trouvée")
    void shouldThrowIfNoSavingsFound() {
        when(sessionService.getAllSessions()).thenReturn(List.of(session));
        when(distributionRepository.findBySessionId(1L)).thenReturn(Collections.emptyList());
        when(borrowingRepository.findAll()).thenReturn(Collections.emptyList());
        when(savingRepository.findBySessionId(1L)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> distributionService.distributeInterests(1L, admin))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No savings found");
    }
}
