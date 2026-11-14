package com.daesabu.meongcoach.support.adapter.persistence;

import com.daesabu.meongcoach.support.application.required.ChatRepository;
import com.daesabu.meongcoach.support.domain.ChatConversation;
import com.daesabu.meongcoach.support.domain.ChatMessage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaChatRepository implements ChatRepository {
	private final ChatConversationJpaRepository conversations;
	private final ChatMessageJpaRepository messages;

	public ChatConversation open(long userId) {
		conversations.insertIfAbsent(userId);
		return conversations.findById(userId).orElseThrow().toDomain();
	}

	public List<ChatConversation> conversations(long before, int limit) {
		var page = PageRequest.of(0, limit);
		if (before == 0) {
			return conversations.findAllByOrderByUpdatedAtDescIdDesc(page).stream()
					.map(ChatConversationEntity::toDomain).toList();
		}
		return conversations.findPrevious(before, page).stream()
				.map(ChatConversationEntity::toDomain).toList();
	}

	public boolean exists(long id) {
		return conversations.existsById(id);
	}

	public void lock(long id) {
		conversations.findLockedById(id).orElseThrow();
	}

	public List<ChatMessage> latestMessages(long id, int limit) {
		var page = PageRequest.of(0, limit);
		return messages.findByConversationIdOrderByIdDesc(id, page).reversed().stream()
				.map(ChatMessageEntity::toDomain).toList();
	}

	public List<ChatMessage> newMessages(long id, long after, int limit) {
		var page = PageRequest.of(0, limit);
		return messages.findByConversationIdAndIdGreaterThanOrderByIdAsc(id, after, page).stream()
				.map(ChatMessageEntity::toDomain).toList();
	}

	public List<ChatMessage> previousMessages(long id, long before, int limit) {
		var page = PageRequest.of(0, limit);
		return messages.findByConversationIdAndIdLessThanOrderByIdDesc(id, before, page).reversed().stream()
				.map(ChatMessageEntity::toDomain).toList();
	}

	public Optional<ChatMessage> find(long id, long sender, UUID clientId) {
		return messages.findByConversationIdAndSenderIdAndClientMessageId(id, sender, clientId)
				.map(ChatMessageEntity::toDomain);
	}

	public ChatMessage save(long id, long sender, boolean operator, UUID clientId, String body) {
		conversations.findById(id).orElseThrow().recordMessage();
		var message = new ChatMessageEntity(id, sender, operator, clientId, body);
		return messages.save(message).toDomain();
	}
}
