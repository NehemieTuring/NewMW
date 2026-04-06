package com.mutuelle.repository;

import com.mutuelle.entity.HelpType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HelpTypeRepository extends JpaRepository<HelpType, Long> {
    List<HelpType> findByActiveTrue();
}
