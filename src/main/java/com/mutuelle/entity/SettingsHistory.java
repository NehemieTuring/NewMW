package com.mutuelle.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "settings_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettingsHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_name", nullable = false, length = 50)
    private String settingName;

    @Column(name = "old_value", precision = 15, scale = 2)
    private BigDecimal oldValue;

    @Column(name = "new_value", precision = 15, scale = 2, nullable = false)
    private BigDecimal newValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modified_by", nullable = false)
    private Administrator modifiedBy;

    @CreationTimestamp
    @Column(name = "modified_date", updatable = false)
    private LocalDateTime modifiedDate;
}
