package com.mutuelle.service;

import com.mutuelle.entity.*;
import com.mutuelle.enums.CashboxName;
import com.mutuelle.exception.BusinessException;
import com.mutuelle.repository.AgapeRepository;
import com.mutuelle.repository.CashboxRepository;
import com.mutuelle.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AgapeService — Tests Unitaires")
class AgapeServiceTest {

    @Mock private AgapeRepository agapeRepository;
    @Mock private CashboxRepository cashboxRepository;
    @Mock private SessionRepository sessionRepository;
    @Mock private TransactionService transactionService;

    @InjectMocks
    private AgapeService agapeService;

    private Administrator admin;
    private Cashbox inscriptionBox;
    private Session session;

    @BeforeEach
    void setUp() {
        admin = Administrator.builder().id(1L).build();
        inscriptionBox = Cashbox.builder().id(1L).name(CashboxName.INSCRIPTION).balance(new BigDecimal("100000")).build();
        session = Session.builder().id(1L).build();
    }

    @Test
    @DisplayName("Création d'agape : déduction de la caisse inscription")
    void shouldCreateAgapeAndDeductFromInscriptionFund() {
        when(cashboxRepository.findByName(CashboxName.INSCRIPTION)).thenReturn(Optional.of(inscriptionBox));
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(agapeRepository.save(any(Agape.class))).thenAnswer(i -> i.getArgument(0));

        Agape result = agapeService.createAgape("Repas fin d'année", "Description", null, LocalDate.now(), 1L, admin);

        // Montant fixe = 45 000
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("45000"));
        verify(transactionService).recordTransaction(eq(new BigDecimal("-45000.00")), eq("AGAPE"), anyString(), eq(inscriptionBox), isNull());
    }

    @Test
    @DisplayName("Erreur si fonds d'inscription insuffisants")
    void shouldThrowIfInscriptionFundInsufficient() {
        inscriptionBox.setBalance(new BigDecimal("10000")); // Requis : 45 000
        when(cashboxRepository.findByName(CashboxName.INSCRIPTION)).thenReturn(Optional.of(inscriptionBox));

        assertThatThrownBy(() -> agapeService.createAgape("Test", "Test", null, LocalDate.now(), 1L, admin))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Fonds insuffisants");
    }
}
