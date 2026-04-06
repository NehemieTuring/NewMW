package com.mutuelle.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "refueling_distribution")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefuelingDistribution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refueling_id", nullable = false)
    private Refueling refueling;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "amount_received", precision = 15, scale = 2, nullable = false)
    private BigDecimal amountReceived;

    @Column(name = "is_in_rule", nullable = false)
    private boolean inRule = true;

    @CreationTimestamp
    @Column(name = "distributed_at", updatable = false)
    private LocalDateTime distributedAt;
}
