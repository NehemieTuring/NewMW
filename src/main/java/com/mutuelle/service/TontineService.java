package com.mutuelle.service;

import com.mutuelle.entity.Tontine;
import com.mutuelle.repository.TontineRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TontineService {
    private final TontineRepository tontineRepository;

    public List<Tontine> getAllTontines() {
        return tontineRepository.findAll();
    }

    public List<Tontine> getActiveTontines() {
        return tontineRepository.findByActiveTrue();
    }

    public Tontine saveTontine(Tontine tontine) {
        return tontineRepository.save(tontine);
    }

    @PostConstruct
    public void initData() {
        if (tontineRepository.count() == 0) {
            tontineRepository.save(Tontine.builder()
                    .name("Tontine Hebdomadaire")
                    .totalAmount(new BigDecimal("1000000"))
                    .currentAmount(new BigDecimal("500000"))
                    .memberCount(20)
                    .active(true)
                    .build());
            
            tontineRepository.save(Tontine.builder()
                    .name("Tontine Mensuelle")
                    .totalAmount(new BigDecimal("500000"))
                    .currentAmount(new BigDecimal("200000"))
                    .memberCount(15)
                    .active(true)
                    .build());
        }
    }
}
