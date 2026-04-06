package com.mutuelle.entity;

import com.mutuelle.enums.MemberStatus; // This is a bit of a stretch, schema says UP_TO_DATE, etc. but I'll use a string or another enum if needed.
// Actually, the schema says ENUM('UP_TO_DATE', 'LATE', 'CRITICAL')
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "solidarity_debt")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolidarityDebt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    @Column(name = "total_due", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalDue = new BigDecimal("150000.00");

    @Column(name = "total_paid", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalPaid = BigDecimal.ZERO;

    @Column(name = "remaining_debt", precision = 15, scale = 2, nullable = false)
    private BigDecimal remainingDebt = new BigDecimal("150000.00");

    @Column(name = "last_payment_date")
    private LocalDate lastPaymentDate;

    @Column(nullable = false)
    private String status = "UP_TO_DATE";

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
