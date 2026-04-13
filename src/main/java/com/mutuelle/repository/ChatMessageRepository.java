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

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT CASE WHEN m.sender.id = :userId THEN m.receiver ELSE m.sender END FROM ChatMessage m WHERE m.sender.id = :userId OR m.receiver.id = :userId")
    List<com.mutuelle.entity.User> findExchangedUsers(@org.springframework.data.repository.query.Param("userId") Long userId);
}
