package com.daesabu.meongcoach.community.domain;

import static java.util.Objects.requireNonNull;

import com.daesabu.meongcoach.community.domain.exception.InvalidPostContentException;
import com.daesabu.meongcoach.shared.domain.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "posts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseEntity {

	public static final int MAX_CONTENT_LENGTH = 500;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "mention_id", unique = true)
	private PostMention mention;

	public static Post write(PostWriteCommand command) {
		Post post = new Post();
		post.userId = requireNonNull(command.userId());
		post.content = validContent(command.content());
		post.mention = mentionOf(command);
		return post;
	}

	private static PostMention mentionOf(PostWriteCommand command) {
		if (!command.hasMention()) {
			return null;
		}
		return PostMention.create(command.mention());
	}

	private static String validContent(String content) {
		if (content == null || content.isBlank()) {
			throw new InvalidPostContentException("본문이 비어 있습니다.");
		}
		if (content.length() > MAX_CONTENT_LENGTH) {
			throw new InvalidPostContentException("본문은 " + MAX_CONTENT_LENGTH + "자를 넘을 수 없습니다.");
		}
		return content;
	}
}
