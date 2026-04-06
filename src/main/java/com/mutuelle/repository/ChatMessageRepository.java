package com.mutuelle.repository;

import com.mutuelle.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findBySenderIdAndReceiverIdOrSenderIdAndReceiverIdOrderByCreatedAtAsc(Long s1, Long r1, Long s2, Long r2);
    List<ChatMessage> findByReceiverIdAndReadFalse(Long receiverId);
    long countByReceiverIdAndReadFalse(Long receiverId);
}
