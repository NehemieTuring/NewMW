package com.mutuelle.service;

import com.mutuelle.entity.Cashbox;
import com.mutuelle.entity.Member;
import com.mutuelle.entity.TransactionLog;
import com.mutuelle.enums.TransactionType;
import com.mutuelle.repository.CashboxRepository;
import com.mutuelle.repository.TransactionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Service to handle global financial transactions and logging across cashboxes.
 */
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionLogRepository transactionLogRepository;
    private final CashboxRepository cashboxRepository;
    private final Clock clock;

    /**
     * Records a transaction, updates the cashbox balance, and logs it.
     * 
     * @param amount      The amount of the transaction. Positive for INFLOW, negative for OUTFLOW.
     * @param category    The category of the transaction (e.g., AGAPE, SAVING_DEPOSIT).
     * @param description A brief description of the transaction.
     * @param cashbox     The cashbox involved in the transaction.
     * @param member      The member involved (optional).
     */
    @Transactional
    public void recordTransaction(BigDecimal amount, String category, String description, Cashbox cashbox, Member member) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        // 1. Update cashbox balance
        cashbox.setBalance(cashbox.getBalance().add(amount));
        cashboxRepository.save(cashbox);

        // 2. Determine transaction type based on amount sign
        TransactionType type = amount.compareTo(BigDecimal.ZERO) > 0 ? TransactionType.INFLOW : TransactionType.OUTFLOW;

        // 3. Create and save transaction log
        TransactionLog log = TransactionLog.builder()
                .transactionDate(LocalDateTime.now(clock))
                .cashbox(cashbox)
                .member(member)
                .amount(amount.abs()) // Absolute value for the amount field, direction is given by type
                .type(type)
                .category(category)
                .description(description)
                .build();

        transactionLogRepository.save(log);
    }
}
