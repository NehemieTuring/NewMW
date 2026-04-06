package com.mutuelle.repository;

import com.mutuelle.entity.MemberStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberStatusLogRepository extends JpaRepository<MemberStatusLog, Long> {
    List<MemberStatusLog> findByMemberIdOrderByCalculatedAtDesc(Long memberId);
}
