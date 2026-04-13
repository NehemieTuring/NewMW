package com.mutuelle.repository;

import com.mutuelle.entity.Tontine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TontineRepository extends JpaRepository<Tontine, Long> {
    List<Tontine> findByActiveTrue();
}
