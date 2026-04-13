package com.mutuelle.repository;

import com.mutuelle.entity.Agape;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgapeRepository extends JpaRepository<Agape, Long> {
    List<Agape> findBySessionId(Long sessionId);
}
