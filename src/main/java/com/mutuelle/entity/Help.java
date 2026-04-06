package com.mutuelle.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "help")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Help {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "help_type_id", nullable = false)
    private HelpType helpType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "administrator_id", nullable = false)
    private Administrator administrator;

    @Column(name = "unit_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal unitAmount;

    @Column(name = "target_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal targetAmount;

    @Column(name = "collected_amount", precision = 15, scale = 2)
    private BigDecimal collectedAmount = BigDecimal.ZERO;

    @Column(name = "limit_date", nullable = false)
    private LocalDateTime limitDate;

    @Column(nullable = false)
    private String status = "ACTIVE";

    @Column(name = "eligible_verified")
    private boolean eligibleVerified = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private Administrator verifiedBy;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
