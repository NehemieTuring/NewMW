package com.mutuelle.repository;

import com.mutuelle.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findBySenderIdAndReceiverIdOrSenderIdAndReceiverIdOrderByCreatedAtAsc(Long s1, Long r1, Long s2, Long r2);
    
    @org.springframework.data.jpa.repository.Query("SELECT m FROM ChatMessage m WHERE m.receiver IS NULL ORDER BY m.createdAt DESC")
    org.springframework.data.domain.Page<ChatMessage> findGroupHistory(org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT m FROM ChatMessage m WHERE m.receiver IS NULL AND LOWER(m.message) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY m.createdAt DESC")
    List<ChatMessage> searchInGroup(@org.springframework.data.repository.query.Param("query") String query);

    @org.springframework.data.jpa.repository.Query("SELECT m FROM ChatMessage m WHERE (m.sender.id = :u1 AND m.receiver.id = :u2) OR (m.sender.id = :u2 AND m.receiver.id = :u1) ORDER BY m.createdAt DESC")
    org.springframework.data.domain.Page<ChatMessage> findChatHistory(@org.springframework.data.repository.query.Param("u1") Long u1, @org.springframework.data.repository.query.Param("u2") Long u2, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT m FROM ChatMessage m WHERE ((m.sender.id = :u1 AND m.receiver.id = :u2) OR (m.sender.id = :u2 AND m.receiver.id = :u1)) AND LOWER(m.message) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY m.createdAt DESC")
    List<ChatMessage> searchInConversation(@org.springframework.data.repository.query.Param("u1") Long u1, @org.springframework.data.repository.query.Param("u2") Long u2, @org.springframework.data.repository.query.Param("query") String query);

    List<ChatMessage> findByReceiverIdAndReadFalse(Long receiverId);
    List<ChatMessage> findByReceiverIdAndSenderIdAndReadFalse(Long receiverId, Long senderId);
    long countByReceiverIdAndReadFalse(Long receiverId);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT u FROM User u WHERE u.id IN (SELECT m.sender.id FROM ChatMessage m WHERE m.receiver.id = :userId) OR u.id IN (SELECT m.receiver.id FROM ChatMessage m WHERE m.sender.id = :userId)")
    List<com.mutuelle.entity.User> findExchangedUsers(@org.springframework.data.repository.query.Param("userId") Long userId);
}
