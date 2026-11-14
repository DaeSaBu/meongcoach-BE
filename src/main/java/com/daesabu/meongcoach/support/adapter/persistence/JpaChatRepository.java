package com.daesabu.meongcoach.support.adapter.persistence;

import com.daesabu.meongcoach.support.application.required.ChatRepository;
import com.daesabu.meongcoach.support.domain.ChatConversation;
import com.daesabu.meongcoach.support.domain.ChatMessage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaChatRepository implements ChatRepository {
	private final EntityManager em;

	public ChatConversation open(long userId) {
		em.createNativeQuery("""
				INSERT INTO support_conversations (id, updated_at)
				VALUES (:id, current_timestamp) ON CONFLICT DO NOTHING
				""")
				.setParameter("id", userId).executeUpdate();
		return em.find(ChatConversationEntity.class, userId).toDomain();
	}

	public List<ChatConversation> conversations(long before, int limit) {
		String query = "SELECT new com.daesabu.meongcoach.support.domain.ChatConversation(c.id, c.updatedAt) "
				+ "FROM SupportConversation c ";
		if (before == 0) {
			return em.createQuery(query + "ORDER BY c.updatedAt DESC, c.id DESC", ChatConversation.class)
					.setMaxResults(limit).getResultList();
		}
		return em.createQuery(query + """
				WHERE c.updatedAt < (SELECT p.updatedAt FROM SupportConversation p WHERE p.id = :before)
				OR (c.updatedAt = (SELECT p.updatedAt FROM SupportConversation p WHERE p.id = :before) AND c.id < :before)
				ORDER BY c.updatedAt DESC, c.id DESC
				""", ChatConversation.class)
				.setParameter("before", before).setMaxResults(limit).getResultList();
	}

	public boolean exists(long id) {
		return em.find(ChatConversationEntity.class, id) != null;
	}

	public void lock(long id) {
		em.find(ChatConversationEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
	}

	public List<ChatMessage> latestMessages(long id, int limit) {
		return em.createQuery("""
				FROM SupportMessage m
				WHERE m.conversationId = :id ORDER BY m.id DESC
				""", ChatMessageEntity.class)
				.setParameter("id", id).setMaxResults(limit).getResultList().reversed().stream()
				.map(ChatMessageEntity::toDomain).toList();
	}

	public List<ChatMessage> newMessages(long id, long after, int limit) {
		return em.createQuery("""
				FROM SupportMessage m
				WHERE m.conversationId = :id AND m.id > :after ORDER BY m.id
				""", ChatMessageEntity.class)
				.setParameter("id", id).setParameter("after", after).setMaxResults(limit).getResultList().stream()
				.map(ChatMessageEntity::toDomain).toList();
	}

	public List<ChatMessage> previousMessages(long id, long before, int limit) {
		return em.createQuery("""
				FROM SupportMessage m
				WHERE m.conversationId = :id AND m.id < :before ORDER BY m.id DESC
				""", ChatMessageEntity.class)
				.setParameter("id", id).setParameter("before", before).setMaxResults(limit)
				.getResultList().reversed().stream()
				.map(ChatMessageEntity::toDomain).toList();
	}

	public Optional<ChatMessage> find(long id, long sender, UUID clientId) {
		return em.createQuery("""
				FROM SupportMessage m
				WHERE m.conversationId = :id AND m.senderId = :sender AND m.clientMessageId = :clientId
				""", ChatMessageEntity.class)
				.setParameter("id", id).setParameter("sender", sender).setParameter("clientId", clientId)
				.getResultList().stream().findFirst().map(ChatMessageEntity::toDomain);
	}

	public ChatMessage save(long id, long sender, boolean operator, UUID clientId, String body) {
		em.find(ChatConversationEntity.class, id).recordMessage();
		var message = new ChatMessageEntity(id, sender, operator, clientId, body);
		em.persist(message);
		return message.toDomain();
	}
}
