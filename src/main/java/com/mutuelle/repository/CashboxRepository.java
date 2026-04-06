package com.mutuelle.repository;

import com.mutuelle.entity.Cashbox;
import com.mutuelle.enums.CashboxName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CashboxRepository extends JpaRepository<Cashbox, Long> {
    Optional<Cashbox> findByName(CashboxName name);
}
