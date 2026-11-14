package com.daesabu.meongcoach.support.application;

import com.daesabu.meongcoach.media.application.provided.MediaDownloadUrlIssuer;
import com.daesabu.meongcoach.media.application.provided.MediaUploadUrlIssuer;
import com.daesabu.meongcoach.media.application.provided.dto.MediaUploadRequest;
import com.daesabu.meongcoach.media.application.provided.dto.MediaUploadUrlResult;
import com.daesabu.meongcoach.support.application.provided.ChatFinder;
import com.daesabu.meongcoach.support.application.provided.ChatMediaUrlIssuer;
import com.daesabu.meongcoach.support.application.provided.dto.ChatMediaUploadRequest;
import com.daesabu.meongcoach.support.application.required.ChatMessageRepository;
import com.daesabu.meongcoach.support.domain.ChatAttachmentType;
import com.daesabu.meongcoach.support.domain.exception.ChatAccessDeniedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Validated
public class ChatMediaUrlIssueService implements ChatMediaUrlIssuer {
	private final ChatFinder finder;
	private final ChatMessageRepository messages;
	private final MediaUploadUrlIssuer uploads;
	private final MediaDownloadUrlIssuer downloads;

	@Override
	public MediaUploadUrlResult upload(Long userId, long conversationId, ChatMediaUploadRequest request) {
		finder.findConversation(userId, conversationId);
		var type = ChatAttachmentType.from(request.mediaType());
		var upload = new MediaUploadRequest(type.getScope(), userId, request.contentType(), request.fileSizeBytes(), type.getMaxFileSizeBytes());
		return uploads.issueToGcs(upload);
	}

	@Override
	public String download(Long userId, long conversationId, long messageId) {
		finder.findConversation(userId, conversationId);
		var message = messages.findByIdAndConversationId(messageId, conversationId).orElseThrow(ChatAccessDeniedException::new);
		var attachment = message.getAttachment();
		if (attachment == null) {
			throw new ChatAccessDeniedException();
		}
		return downloads.issue(attachment.getType().getScope(), attachment.getObjectKey());
	}
}
