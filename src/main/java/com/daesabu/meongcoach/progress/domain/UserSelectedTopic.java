package com.daesabu.meongcoach.progress.domain;

import com.daesabu.meongcoach.shared.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자가 마지막으로 선택한 토픽. 커리큘럼 진입 시 어느 토픽 화면을 보여줄지 판단한다(U-0201).
 */
@Getter
@Entity
@Table(
		name = "user_selected_topic",
		uniqueConstraints = @UniqueConstraint(columnNames = "user_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSelectedTopic extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false)
	private Long topicId;

	public static UserSelectedTopic enter(Long userId, Long topicId) {
		UserSelectedTopic selectedTopic = new UserSelectedTopic();
		selectedTopic.userId = userId;
		selectedTopic.topicId = topicId;
		return selectedTopic;
	}

	/**
	 * 선택 토픽을 옮긴다. 같은 토픽이면 값이 바뀌지 않아 더티 체킹이 일어나지 않고 UPDATE도 나가지 않는다.
	 */
	public void moveTo(Long topicId) {
		this.topicId = topicId;
	}
}
