package com.mutuelle.entity;

import com.mutuelle.enums.MemberStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "member_status_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberStatusLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberStatus status;

    @Column(name = "solidarity_debt", precision = 15, scale = 2, nullable = false)
    private BigDecimal solidarityDebt;

    @Column(name = "refueling_debt", precision = 15, scale = 2, nullable = false)
    private BigDecimal refuelingDebt;

    @Column(name = "borrowing_debt", precision = 15, scale = 2, nullable = false)
    private BigDecimal borrowingDebt;

    @Column(name = "total_debt", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalDebt;

    @CreationTimestamp
    @Column(name = "calculated_at", updatable = false)
    private LocalDateTime calculatedAt;
}
