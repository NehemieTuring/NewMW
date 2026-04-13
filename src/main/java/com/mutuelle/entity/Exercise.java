package com.mutuelle.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

    @Column(name = "year", nullable = false, length = 10)
    private String year;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "interest_rate", precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "solidarity_amount", precision = 15, scale = 2)
    private BigDecimal solidarityAmount;

    @Column(name = "agape_amount", precision = 15, scale = 2)
    private BigDecimal agapeAmount;

    @Column(name = "penalty_amount", precision = 15, scale = 2)
    private BigDecimal penaltyAmount;

    @Column(name = "inscription_amount", precision = 15, scale = 2)
    private BigDecimal inscriptionAmount;

    @Column(nullable = false)
    private boolean active = false;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "administrator_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "user"})
    private Administrator administrator;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
