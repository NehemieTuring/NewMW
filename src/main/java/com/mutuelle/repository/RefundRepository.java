package com.mutuelle.repository;

import com.mutuelle.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {
    List<Refund> findByBorrowingId(Long borrowingId);
    List<Refund> findByMemberId(Long memberId);
}
