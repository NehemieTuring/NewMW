package com.mutuelle.repository;

import com.mutuelle.entity.TransactionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionLogRepository extends JpaRepository<TransactionLog, Long> {
    List<TransactionLog> findByMemberId(Long memberId);
    List<TransactionLog> findByCashboxId(Long cashboxId);
}
