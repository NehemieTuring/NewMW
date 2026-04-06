package com.mutuelle.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "interest_distribution")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterestDistribution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrowing_id", nullable = false)
    private Borrowing borrowing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "administrator_id", nullable = false)
    private Administrator administrator;

    @Column(name = "total_interest", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalInterest;

    @Column(name = "distributed_amount", precision = 15, scale = 2)
    private BigDecimal distributedAmount = BigDecimal.ZERO;

    @Column(name = "remaining_amount", precision = 15, scale = 2)
    private BigDecimal remainingAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private String status = "PENDING";

    @Column(name = "distribution_date")
    private LocalDateTime distributionDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
