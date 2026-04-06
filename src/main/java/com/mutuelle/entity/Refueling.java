package com.mutuelle.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "refueling")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Refueling {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "administrator_id", nullable = false)
    private Administrator administrator;

    @Column(name = "total_outflows", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalOutflows;

    @Column(name = "eligible_member_count", nullable = false)
    private Integer eligibleMemberCount;

    @Column(name = "amount_per_member", precision = 15, scale = 2, nullable = false)
    private BigDecimal amountPerMember;

    @Column(name = "total_collected", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalCollected;

    @Column(name = "surplus_to_inscription", precision = 15, scale = 2, nullable = false)
    private BigDecimal surplusToInscription;

    @Column(name = "distribution_date", nullable = false)
    private LocalDate distributionDate;

    @Column(nullable = false)
    private String status = "CALCULATED";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
