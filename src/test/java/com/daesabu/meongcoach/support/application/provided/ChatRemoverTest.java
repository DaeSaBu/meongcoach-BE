package com.daesabu.meongcoach.support.application.provided;

import static org.assertj.core.api.Assertions.*;

import com.daesabu.meongcoach.support.application.provided.dto.ChatMessageRequest;
import com.daesabu.meongcoach.support.application.required.ChatConversationRepository;
import com.daesabu.meongcoach.support.application.required.ChatMessageRepository;
import com.daesabu.meongcoach.support.domain.exception.ChatAccessDeniedException;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.persistence.EntityManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@TestPropertySource(properties = {
		"spring.datasource.url=jdbc:tc:postgresql:18.3:///meongcoach_schema_check",
		"spring.flyway.enabled=true",
		"spring.jpa.hibernate.ddl-auto=validate"
})
@Transactional
@WithMockUser
class ChatRemoverTest {

	@Autowired ChatRemover remover;
	@Autowired ChatRegister register;
	@Autowired ChatFinder finder;
	@Autowired ChatConversationRepository conversations;
	@Autowired ChatMessageRepository messages;
	@Autowired UserRepository users;
	@Autowired JdbcTemplate jdbc;
	@Autowired EntityManager em;

	@Test
	void 나가면_대화와_메시지를_보관하고_다시_열면_새_대화를_만든다() {
		Long owner = users.save(User.registerUser()).getId();
		Long other = users.save(User.registerUser()).getId();
		long id = register.open(owner).getId();
		long otherId = register.open(other).getId();
		var message = register.send(owner, id, new ChatMessageRequest(UUID.randomUUID(), "삭제할 문의"));
		register.send(other, otherId, new ChatMessageRequest(UUID.randomUUID(), "유지할 문의"));

		remover.leave(owner, id);
		conversations.flush();
		em.clear();

		assertThat(conversations.existsById(id)).isFalse();
		assertThat(messages.existsById(message.getId())).isTrue();
		assertThat(jdbc.queryForObject("SELECT deleted_at IS NOT NULL FROM support_conversations WHERE id = ?", Boolean.class, id)).isTrue();
		assertThat(finder.conversations(0, null)).extracting(com.daesabu.meongcoach.support.domain.ChatConversation::getId).doesNotContain(id);
		assertThatThrownBy(() -> finder.latestMessages(owner, id)).isInstanceOf(ChatAccessDeniedException.class);
		assertThat(finder.latestMessages(other, otherId)).hasSize(1);
		long reopenedId = register.open(owner).getId();
		assertThat(reopenedId).isNotEqualTo(id);
		assertThat(finder.latestMessages(owner, reopenedId)).isEmpty();
		assertThatThrownBy(() -> register.send(owner, id, new ChatMessageRequest(UUID.randomUUID(), "늦은 전송")))
				.isInstanceOf(ChatAccessDeniedException.class);
	}

	@Test
	void 다른_회원은_대화를_삭제할_수_없다() {
		Long owner = users.save(User.registerUser()).getId();
		Long other = users.save(User.registerUser()).getId();
		long id = register.open(owner).getId();

		assertThatThrownBy(() -> remover.leave(other, id)).isInstanceOf(ChatAccessDeniedException.class);
		assertThat(conversations.existsById(id)).isTrue();
	}

	@Test
	@WithMockUser(roles = "EXPERT")
	void 상담원도_다른_회원의_대화를_삭제할_수_없다() {
		Long owner = users.save(User.registerUser()).getId();
		Long expert = users.save(User.registerUser()).getId();
		long id = register.open(owner).getId();

		assertThatThrownBy(() -> remover.leave(expert, id)).isInstanceOf(ChatAccessDeniedException.class);
		assertThat(conversations.existsById(id)).isTrue();
	}
}
