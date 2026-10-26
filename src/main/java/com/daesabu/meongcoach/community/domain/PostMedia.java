package com.daesabu.meongcoach.community.domain;

import static java.util.Objects.requireNonNull;

import com.daesabu.meongcoach.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "post_media")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostMedia extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "media_type", nullable = false, length = 20)
	private PostMediaType type;

	@Column(nullable = false, length = 512)
	private String url;

	@Column(nullable = false)
	private int sortOrder;

	static PostMedia create(PostMediaCommand command, int sortOrder) {
		PostMedia media = new PostMedia();
		media.type = requireNonNull(command.type());
		media.url = requireNonNull(command.url());
		media.sortOrder = sortOrder;
		return media;
	}
}
