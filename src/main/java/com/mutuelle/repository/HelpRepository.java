package com.mutuelle.repository;

import com.mutuelle.entity.Help;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HelpRepository extends JpaRepository<Help, Long> {
    List<Help> findByStatus(String status);
}
