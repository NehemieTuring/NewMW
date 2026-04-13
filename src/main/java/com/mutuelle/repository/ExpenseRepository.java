package com.mutuelle.repository;

import com.mutuelle.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByExpenseDateBetween(LocalDateTime start, LocalDateTime end);
    List<Expense> findByCategory(String category);
}
