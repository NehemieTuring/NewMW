package com.mutuelle.service;

import com.mutuelle.entity.Administrator;
import com.mutuelle.entity.Expense;
import com.mutuelle.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;

    @Transactional
    public Expense recordExpense(BigDecimal amount, String reason, String category, Administrator admin, String receiptUrl) {
        Expense expense = Expense.builder()
                .amount(amount)
                .reason(reason)
                .category(category)
                .registeredBy(admin)
                .receiptUrl(receiptUrl)
                .expenseDate(LocalDateTime.now())
                .build();
        return expenseRepository.save(expense);
    }

    public List<Expense> getAllExpenses() {
        return expenseRepository.findAll();
    }

    public List<Expense> getExpensesByPeriod(LocalDateTime start, LocalDateTime end) {
        return expenseRepository.findByExpenseDateBetween(start, end);
    }

    @Transactional
    public void deleteExpense(Long id) {
        expenseRepository.deleteById(id);
    }
}
