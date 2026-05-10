package com.mutuelle.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import org.hibernate.annotations.Formula;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "member")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "administrator_id", nullable = false)
    private Administrator administrator;


    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "inscription_date", nullable = false)
    private LocalDate inscriptionDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Formula("(SELECT COALESCE(SUM(CASE WHEN s.type = 'INFLOW' THEN s.amount ELSE -s.amount END), 0) FROM saving s WHERE s.member_id = id)")
    private BigDecimal savingsTotal;
}
