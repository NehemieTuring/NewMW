package com.mutuelle.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "exercise")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Exercise {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "`year`", nullable = false, length = 10)
    private String year;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "interest_rate", precision = 5, scale = 2)
    private BigDecimal interestRate = new BigDecimal("3.00");

    @Column(name = "inscription_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal inscriptionAmount = new BigDecimal("50000.00");

    @Column(name = "solidarity_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal solidarityAmount = new BigDecimal("150000.00");

    @Column(name = "agape_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal agapeAmount = new BigDecimal("45000.00");

    @Column(name = "penalty_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal penaltyAmount = new BigDecimal("15000.00");

    @Column(nullable = false)
    private boolean active = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "administrator_id", nullable = false)
    private Administrator administrator;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
