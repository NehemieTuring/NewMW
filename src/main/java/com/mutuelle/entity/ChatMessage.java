package com.mutuelle.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_message")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = true)
    private User receiver;

    @Column(nullable = true, length = 2000)
    private String message;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private Boolean read = false;

    @Builder.Default
    @Column(name = "is_delivered", nullable = false)
    private Boolean delivered = false;

    @Builder.Default
    @Column(name = "is_edited", nullable = false)
    private Boolean edited = false;

    private LocalDateTime editedAt;

    private String attachmentUrl;
    
    private String attachmentType; // IMAGE, PDF, etc.

    @Column(name = "external_message_id", length = 100)
    private String externalMessageId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
