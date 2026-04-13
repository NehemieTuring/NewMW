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

    public ChatMessage sendMessage(Long senderId, Long receiverId, String content) {
        User sender = userRepository.findById(senderId).orElseThrow();
        User receiver = userRepository.findById(receiverId).orElseThrow();

        ChatMessage message = ChatMessage.builder()
                .sender(sender)
                .receiver(receiver)
                .message(content)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(message);
        
        // Broadcast to receiver's private queue
        messagingTemplate.convertAndSendToUser(
            receiver.getId().toString(), 
            "/queue/messages", 
            savedMessage
        );
        
        return savedMessage;
    }

    public List<ChatMessage> getMessages(Long userId, Long otherUserId) {
        return chatMessageRepository.findBySenderIdAndReceiverIdOrSenderIdAndReceiverIdOrderByCreatedAtAsc(userId, otherUserId, otherUserId, userId);
    }

    public long getUnreadCount(Long userId) {
        return chatMessageRepository.countByReceiverIdAndReadFalse(userId);
    }

    public void markAsRead(Long messageId) {
        chatMessageRepository.findById(messageId).ifPresent(msg -> {
            msg.setRead(true);
            chatMessageRepository.save(msg);
        });
    }

    public List<User> getConversations(Long userId) {
        // Optimized: get unique counterparts via repository query
        return chatMessageRepository.findExchangedUsers(userId).stream()
                .filter(u -> u.getType() != com.mutuelle.enums.RoleType.SUPER_ADMIN)
                .distinct()
                .toList();
    }
}
