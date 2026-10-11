package com.daesabu.meongcoach.community.domain;

import static java.util.Objects.requireNonNull;

import com.daesabu.meongcoach.community.domain.exception.InvalidPostContentException;
import com.daesabu.meongcoach.shared.domain.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "posts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseEntity {

	public static final int MAX_CONTENT_LENGTH = 2000;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<PostMention> mentions = new ArrayList<>();

	private Post(PostWriteCommand command) {
		this.userId = requireNonNull(command.userId());
		this.content = validContent(command.content());
		requireNonNull(command.mentions()).stream()
				.distinct()
				.map(mention -> new PostMention(this, mention))
				.forEach(this.mentions::add);
	}

	public static Post write(PostWriteCommand command) {
		return new Post(command);
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

	public boolean isWrittenBy(Long userId) {
		return this.userId.equals(userId);
	}

	public List<Long> findMentionedCardIds() {
		return mentions.stream()
				.filter(mention -> mention.isTarget(MentionTargetType.CARD))
				.map(PostMention::getTargetId)
				.toList();
	}
}
