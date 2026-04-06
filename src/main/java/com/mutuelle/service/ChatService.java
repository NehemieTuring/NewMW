package com.mutuelle.service;

import com.mutuelle.entity.ChatMessage;
import com.mutuelle.entity.User;
import com.mutuelle.repository.ChatMessageRepository;
import com.mutuelle.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

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

        return chatMessageRepository.save(message);
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
        // Simple logic: get all messages where user is sender or receiver, extract unique other users
        // This is a bit inefficient without a dedicated query but works for now.
        List<ChatMessage> sent = chatMessageRepository.findAll(); // Should be filtered in a real app
        return sent.stream()
                .filter(m -> m.getSender().getId().equals(userId) || m.getReceiver().getId().equals(userId))
                .map(m -> m.getSender().getId().equals(userId) ? m.getReceiver() : m.getSender())
                .distinct()
                .toList();
    }
}
