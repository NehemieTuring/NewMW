package com.mutuelle.service;

import com.mutuelle.entity.Cashbox;
import com.mutuelle.entity.Member;
import com.mutuelle.entity.TransactionLog;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import com.mutuelle.enums.TransactionType;
import com.mutuelle.repository.CashboxRepository;
import com.mutuelle.repository.TransactionLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService — Tests Unitaires")
class TransactionServiceTest {

    @Mock private TransactionLogRepository transactionLogRepository;
    @Mock private CashboxRepository cashboxRepository;
    @Mock private Clock clock;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    @DisplayName("Enregistrement d'une entrée d'argent (INFLOW)")
    void shouldRecordInflowTransaction() {
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(clock.instant()).thenReturn(Instant.now());

        Cashbox cashbox = Cashbox.builder().id(1L).balance(new BigDecimal("1000")).build();
        Member member = Member.builder().id(1L).build();
        
        transactionService.recordTransaction(new BigDecimal("500"), "DÉPÔT", "Test dépôt", cashbox, member);

        // Vérifie la mise à jour du solde
        assertThat(cashbox.getBalance()).isEqualByComparingTo(new BigDecimal("1500"));
        verify(cashboxRepository).save(cashbox);

        // Vérifie le log
        ArgumentCaptor<TransactionLog> captor = ArgumentCaptor.forClass(TransactionLog.class);
        verify(transactionLogRepository).save(captor.capture());
        TransactionLog log = captor.getValue();
        assertThat(log.getType()).isEqualTo(TransactionType.INFLOW);
        assertThat(log.getAmount()).isEqualByComparingTo(new BigDecimal("500"));
        assertThat(log.getCategory()).isEqualTo("DÉPÔT");
    }

    @Test
    @DisplayName("Enregistrement d'une sortie d'argent (OUTFLOW)")
    void shouldRecordOutflowTransaction() {
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(clock.instant()).thenReturn(Instant.now());

        Cashbox cashbox = Cashbox.builder().id(1L).balance(new BigDecimal("1000")).build();
        
        transactionService.recordTransaction(new BigDecimal("-200"), "ACHAT", "Test achat", cashbox, null);

        assertThat(cashbox.getBalance()).isEqualByComparingTo(new BigDecimal("800"));
        
        ArgumentCaptor<TransactionLog> captor = ArgumentCaptor.forClass(TransactionLog.class);
        verify(transactionLogRepository).save(captor.capture());
        TransactionLog log = captor.getValue();
        assertThat(log.getType()).isEqualTo(TransactionType.OUTFLOW);
        assertThat(log.getAmount()).isEqualByComparingTo(new BigDecimal("200")); // Valeur absolue
    }
}
