package com.mutuelle.repository;

import com.mutuelle.entity.Saving;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SavingRepository extends JpaRepository<Saving, Long> {
    List<Saving> findByMemberId(Long memberId);
    List<Saving> findBySessionId(Long sessionId);
}
