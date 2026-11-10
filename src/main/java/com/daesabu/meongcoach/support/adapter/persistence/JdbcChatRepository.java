package com.daesabu.meongcoach.support.adapter.persistence;

import com.daesabu.meongcoach.support.application.required.ChatRepository;
import com.daesabu.meongcoach.support.domain.ChatConversation;
import com.daesabu.meongcoach.support.domain.ChatMessage;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcChatRepository implements ChatRepository {
	private final JdbcTemplate jdbc;

	public ChatConversation open(long userId) {
		jdbc.update(
				"INSERT INTO support_conversations (id) VALUES (?) ON CONFLICT DO NOTHING", userId);
		return jdbc.queryForObject(
				"SELECT id, updated_at FROM support_conversations WHERE id = ?",
				this::conversation,
				userId);
	}

	public List<ChatConversation> conversations(long before, int limit) {
		if (before == 0) {
			return jdbc.query(
					"SELECT id, updated_at FROM support_conversations ORDER BY updated_at DESC,id"
							+ " DESC LIMIT ?",
					this::conversation,
					limit);
		}
		return jdbc.query(
				"SELECT id, updated_at FROM support_conversations WHERE (updated_at,id) < (SELECT"
					+ " updated_at,id FROM support_conversations WHERE id=?) ORDER BY updated_at"
					+ " DESC,id DESC LIMIT ?",
				this::conversation,
				before,
				limit);
	}

	public boolean exists(long id) {
		Boolean exists =
				jdbc.queryForObject(
						"SELECT EXISTS(SELECT 1 FROM support_conversations WHERE id = ?)",
						Boolean.class,
						id);
		return Boolean.TRUE.equals(exists);
	}

	public void lock(long id) {
		jdbc.queryForObject(
				"SELECT id FROM support_conversations WHERE id = ? FOR UPDATE", Long.class, id);
	}

	public List<ChatMessage> messages(long id, long after, long before, int limit) {
		if (after >= 0) {
			return jdbc.query(
					"SELECT * FROM support_messages WHERE conversation_id = ? AND id > ? ORDER BY"
							+ " id LIMIT ?",
					this::message,
					id,
					after,
					limit);
		}
		if (before > 0) {
			return jdbc.query(
					"SELECT * FROM (SELECT * FROM support_messages WHERE conversation_id = ? AND id"
							+ " < ? ORDER BY id DESC LIMIT ?) recent ORDER BY id",
					this::message,
					id,
					before,
					limit);
		}
		return jdbc.query(
				"SELECT * FROM (SELECT * FROM support_messages WHERE conversation_id = ? ORDER BY"
						+ " id DESC LIMIT ?) recent ORDER BY id",
				this::message,
				id,
				limit);
	}

	public Optional<ChatMessage> find(long id, long sender, UUID clientId) {
		return jdbc
				.query(
						"SELECT * FROM support_messages WHERE conversation_id = ? AND sender_id = ?"
								+ " AND client_message_id = ?",
						this::message,
						id,
						sender,
						clientId)
				.stream()
				.findFirst();
	}

	public ChatMessage save(long id, long sender, boolean operator, UUID clientId, String body) {
		jdbc.update(
				"UPDATE support_conversations SET updated_at = clock_timestamp() WHERE id = ?", id);
		return jdbc.queryForObject(
				"INSERT INTO"
					+ " support_messages(conversation_id,sender_id,operator,client_message_id,body)"
					+ " VALUES (?,?,?,?,?) RETURNING *",
				this::message,
				id,
				sender,
				operator,
				clientId,
				body);
	}

	public void notifyChanged(long id) {
		jdbc.queryForObject("SELECT pg_notify('support_chat', ?)", String.class, Long.toString(id));
	}

	private ChatConversation conversation(ResultSet rs, int n) throws SQLException {
		long id = rs.getLong("id");
		var updatedAt = rs.getTimestamp("updated_at").toInstant();
		return new ChatConversation(id, updatedAt);
	}

	private ChatMessage message(ResultSet rs, int n) throws SQLException {
		long id = rs.getLong("id");
		long conversationId = rs.getLong("conversation_id");
		long senderId = rs.getLong("sender_id");
		boolean operator = rs.getBoolean("operator");
		var clientId = rs.getObject("client_message_id", UUID.class);
		String body = rs.getString("body");
		var createdAt = rs.getTimestamp("created_at").toInstant();
		return new ChatMessage(id, conversationId, senderId, operator, clientId, body, createdAt);
	}
}
