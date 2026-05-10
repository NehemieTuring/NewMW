package com.mutuelle.entity;

import com.mutuelle.enums.SessionState;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "session")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "exercise_id", nullable = false)
    @JsonIgnoreProperties({"sessions", "hibernateLazyInitializer", "handler"})
    private Exercise exercise;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "administrator_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "user"})
    private Administrator administrator;

    @Column(name = "session_number", nullable = false)
    private Integer sessionNumber;

    @Column(length = 100)
    private String name;

    @Column(nullable = false)
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionState state = SessionState.OPEN;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
