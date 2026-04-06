package com.mutuelle.repository;

import com.mutuelle.entity.Solidarity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolidarityRepository extends JpaRepository<Solidarity, Long> {
    List<Solidarity> findByMemberId(Long memberId);
}
