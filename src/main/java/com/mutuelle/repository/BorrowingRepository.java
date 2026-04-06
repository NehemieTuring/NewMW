package com.mutuelle.repository;

import com.mutuelle.entity.Borrowing;
import com.mutuelle.enums.BorrowingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BorrowingRepository extends JpaRepository<Borrowing, Long> {
    List<Borrowing> findByMemberIdAndStatusIn(Long memberId, List<BorrowingStatus> statuses);
    List<Borrowing> findByMemberId(Long memberId);
}
