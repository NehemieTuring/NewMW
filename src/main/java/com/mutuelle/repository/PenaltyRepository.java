package com.mutuelle.repository;

import com.mutuelle.entity.Penalty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PenaltyRepository extends JpaRepository<Penalty, Long> {
    List<Penalty> findByMemberId(Long memberId);
    List<Penalty> findByStatus(String status);
}
