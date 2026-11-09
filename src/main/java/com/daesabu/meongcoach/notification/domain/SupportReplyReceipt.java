package com.daesabu.meongcoach.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "support_reply_receipts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SupportReplyReceipt {
	@Id
	@Column(name = "message_id", length = 200)
	private String messageId;

	@Column(name = "received_at", nullable = false)
	private Instant receivedAt;

	public static SupportReplyReceipt received(String messageId, Instant receivedAt) {
		SupportReplyReceipt receipt = new SupportReplyReceipt();
		receipt.messageId = messageId;
		receipt.receivedAt = receivedAt;
		return receipt;
	}
}
