package com.daesabu.meongcoach.support.application.provided;

import static org.assertj.core.api.Assertions.*;

import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.support.application.provided.dto.ChatAttachmentRequest;
import com.daesabu.meongcoach.support.application.provided.dto.ChatMediaUploadRequest;
import com.daesabu.meongcoach.support.application.provided.dto.ChatMessageRequest;
import com.daesabu.meongcoach.support.domain.exception.ChatAccessDeniedException;
import com.daesabu.meongcoach.support.domain.exception.InvalidChatAttachmentException;
import com.daesabu.meongcoach.support.domain.exception.InvalidChatMessageException;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

@ApplicationTest
@WithMockUser
class ChatMediaUrlIssuerTest {
	@Autowired ChatMediaUrlIssuer media;
	@Autowired ChatRegister chats;
	@Autowired ChatFinder finder;
	@Autowired ChatRemover remover;
	@Autowired UserRepository users;

	@Test
	void 사진과_영상은_회원별_비공개_경로로_업로드한다() {
		long userId = users.save(User.registerUser()).getId();
		long id = chats.open(userId).getId();

		var image = media.upload(userId, id, new ChatMediaUploadRequest("IMAGE", "image/jpeg", 1024L));
		var video = media.upload(userId, id, new ChatMediaUploadRequest("VIDEO", "video/mp4", 2048L));

		assertThat(image.objectKey()).startsWith("images/support-chat/" + userId + "/").endsWith(".jpg");
		assertThat(video.objectKey()).startsWith("videos/support-chat/" + userId + "/").endsWith(".mp4");
		assertThat(image.uploadUrl()).contains("X-Goog-Signature=");
	}

	@Test
	void 첨부만_보내도_저장되며_같은_UUID의_재시도는_중복되지_않는다() {
		long userId = users.save(User.registerUser()).getId();
		long id = chats.open(userId).getId();
		var uploaded = media.upload(userId, id, new ChatMediaUploadRequest("IMAGE", "image/jpeg", 1024L));
		var request = new ChatMessageRequest(UUID.randomUUID(), "", new ChatAttachmentRequest("IMAGE", uploaded.objectKey()));

		var message = chats.send(userId, id, request);

		assertThat(message.getBody()).isEmpty();
		assertThat(message.getAttachment().getObjectKey()).isEqualTo(uploaded.objectKey());
		assertThat(chats.send(userId, id, request).getId()).isEqualTo(message.getId());
		assertThat(finder.latestMessages(userId, id)).hasSize(1);
		assertThat(media.download(userId, id, message.getId())).contains("X-Goog-Signature=");
	}

	@Test
	void 글과_첨부가_모두_없으면_보낼_수_없다() {
		long userId = users.save(User.registerUser()).getId();
		long id = chats.open(userId).getId();

		assertThatThrownBy(() -> chats.send(userId, id, new ChatMessageRequest(UUID.randomUUID(), " ")))
				.isInstanceOf(InvalidChatMessageException.class);
	}

	@Test
	void 다른_회원의_파일을_첨부할_수_없다() {
		long userId = users.save(User.registerUser()).getId();
		long id = chats.open(userId).getId();
		var attachment = new ChatAttachmentRequest("IMAGE", "images/support-chat/99999999/foreign.jpg");

		assertThatThrownBy(() -> chats.send(userId, id, new ChatMessageRequest(UUID.randomUUID(), "사진", attachment)))
				.isInstanceOf(InvalidChatAttachmentException.class);
	}

	@Test
	void 다른_대화의_업로드와_첨부_열람은_거절한다() {
		long owner = users.save(User.registerUser()).getId();
		long other = users.save(User.registerUser()).getId();
		long id = chats.open(owner).getId();
		long otherId = chats.open(other).getId();
		var uploaded = media.upload(owner, id, new ChatMediaUploadRequest("IMAGE", "image/jpeg", 1024L));
		var message = chats.send(owner, id, new ChatMessageRequest(UUID.randomUUID(), "", new ChatAttachmentRequest("IMAGE", uploaded.objectKey())));

		assertThatThrownBy(() -> media.upload(other, id, new ChatMediaUploadRequest("VIDEO", "video/mp4", 1024L)))
				.isInstanceOf(ChatAccessDeniedException.class);
		assertThatThrownBy(() -> media.download(other, id, message.getId())).isInstanceOf(ChatAccessDeniedException.class);
		assertThatThrownBy(() -> media.download(other, otherId, message.getId())).isInstanceOf(ChatAccessDeniedException.class);
		remover.leave(owner, id);
		assertThatThrownBy(() -> media.download(owner, id, message.getId())).isInstanceOf(ChatAccessDeniedException.class);
	}

	@Test
	void 지원하지_않는_종류는_거절한다() {
		long userId = users.save(User.registerUser()).getId();
		long id = chats.open(userId).getId();

		assertThatThrownBy(() -> media.upload(userId, id, new ChatMediaUploadRequest("AUDIO", "audio/mpeg", 1024L)))
				.isInstanceOf(InvalidChatAttachmentException.class);
	}
}
