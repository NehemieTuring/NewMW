package com.mutuelle.service;

import com.mutuelle.entity.ChatMessage;
import com.mutuelle.entity.User;
import com.mutuelle.repository.ChatMessageRepository;
import com.mutuelle.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    public ChatMessage sendMessage(Long senderId, Long receiverId, String content, String attachmentUrl, String attachmentType) {
        User sender = userRepository.findById(senderId).orElseThrow();
        User receiver = receiverId != null ? userRepository.findById(receiverId).orElse(null) : null;

        ChatMessage message = ChatMessage.builder()
                .sender(sender)
                .receiver(receiver)
                .message(content)
                .attachmentUrl(attachmentUrl)
                .attachmentType(attachmentType)
                .read(false)
                .delivered(true)
                .edited(false)
                .createdAt(LocalDateTime.now())
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(message);
        
        // Update sender's last seen
        sender.setLastSeen(LocalDateTime.now());
        userRepository.save(sender);

        if (receiver != null) {
            // Private message: Send to receiver
            messagingTemplate.convertAndSendToUser(
                receiver.getId().toString(), 
                "/queue/messages", 
                savedMessage
            );
            // Also notify receiver of unread count update
            notifyUnreadCount(receiver.getId());

            // Also send back to sender so they see it in real-time
            messagingTemplate.convertAndSendToUser(
                sender.getId().toString(),
                "/queue/messages",
                savedMessage
            );
        } else {
            // Group message: Broadcast to everyone
            messagingTemplate.convertAndSend("/topic/group.messages", savedMessage);
        }
        
        return savedMessage;
    }

    private void notifyUnreadCount(Long userId) {
        long count = getUnreadCount(userId);
        messagingTemplate.convertAndSendToUser(
            userId.toString(),
            "/queue/unread",
            count
        );
    }

    public org.springframework.data.domain.Page<ChatMessage> getGroupHistory(int page, int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return chatMessageRepository.findGroupHistory(pageable);
    }

    public List<ChatMessage> searchGroupMessages(String query) {
        return chatMessageRepository.searchInGroup(query);
    }

    public List<ChatMessage> getMessages(Long userId, Long otherUserId) {
        return chatMessageRepository.findBySenderIdAndReceiverIdOrSenderIdAndReceiverIdOrderByCreatedAtAsc(userId, otherUserId, otherUserId, userId);
    }

    public org.springframework.data.domain.Page<ChatMessage> getChatHistory(Long userId, Long otherUserId, int page, int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return chatMessageRepository.findChatHistory(userId, otherUserId, pageable);
    }

    public List<ChatMessage> searchMessages(Long userId, Long otherUserId, String query) {
        return chatMessageRepository.searchInConversation(userId, otherUserId, query);
    }

    @Transactional
    public ChatMessage editMessage(Long userId, Long messageId, String newContent) {
        ChatMessage msg = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new com.mutuelle.exception.BusinessException("Message introuvable"));
        
        if (!msg.getSender().getId().equals(userId)) {
            throw new com.mutuelle.exception.BusinessException("Vous ne pouvez modifier que vos propres messages");
        }

        // Capture receiver before save
        User receiver = msg.getReceiver();

        msg.setMessage(newContent);
        msg.setEdited(true);
        msg.setEditedAt(LocalDateTime.now());
        
        ChatMessage saved = chatMessageRepository.save(msg);
        
        // Notify of update
        if (receiver != null) {
            messagingTemplate.convertAndSendToUser(
                receiver.getId().toString(),
                "/queue/messages/updates",
                saved
            );
        } else {
            messagingTemplate.convertAndSend("/topic/group.messages.updates", saved);
        }
        
        return saved;
    }

    @Transactional
    public void deleteMessage(Long userId, Long messageId) {
        ChatMessage msg = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new com.mutuelle.exception.BusinessException("Message introuvable"));
        
        if (!msg.getSender().getId().equals(userId)) {
            throw new com.mutuelle.exception.BusinessException("Vous ne pouvez supprimer que vos propres messages");
        }

        // Capture receiver before delete
        User receiver = msg.getReceiver();

        chatMessageRepository.delete(msg);

        // Notify of deletion
        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("id", messageId);
        payload.put("action", "DELETE");
        
        if (receiver != null) {
            messagingTemplate.convertAndSendToUser(
                receiver.getId().toString(),
                "/queue/messages/updates",
                payload
            );
        } else {
            messagingTemplate.convertAndSend("/topic/group.messages.updates", payload);
        }
    }

    public void updateLastSeen(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastSeen(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    public long getUnreadCount(Long userId) {
        return chatMessageRepository.countByReceiverIdAndReadFalse(userId);
    }

    public void markAsRead(Long messageId) {
        chatMessageRepository.findById(messageId).ifPresent(msg -> {
            msg.setRead(true);
            chatMessageRepository.save(msg);
            notifyUnreadCount(msg.getReceiver().getId());
        });
    }

    public void markConversationAsRead(Long receiverId, Long senderId) {
        List<ChatMessage> unread = chatMessageRepository.findByReceiverIdAndSenderIdAndReadFalse(receiverId, senderId);
        if (!unread.isEmpty()) {
            unread.forEach(m -> m.setRead(true));
            chatMessageRepository.saveAll(unread);
            notifyUnreadCount(receiverId);
        }
    }

    public List<User> getConversations(Long userId) {
        // ... (existing code is used in context)
        return chatMessageRepository.findExchangedUsers(userId).stream()
                .filter(u -> !u.getId().equals(userId))
                .filter(u -> u.getType() != com.mutuelle.enums.RoleType.SUPER_ADMIN)
                .distinct()
                .toList();
    }
}
