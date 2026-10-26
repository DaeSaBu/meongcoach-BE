package com.daesabu.meongcoach.community.application;

import com.daesabu.meongcoach.community.application.provided.dto.MentionTargetResult;
import com.daesabu.meongcoach.community.domain.MentionTargetType;
import java.util.Collection;
import java.util.List;

/**
 * 멘션 대상 종류 하나의 요약을 조회한다.
 */
public interface MentionTargetReader {

	/**
	 * 이 리더가 담당하는 멘션 대상 종류를 반환한다.
	 */
	MentionTargetType targetType();

	/**
	 * 대상 ID 목록에 해당하는 대상의 요약을 반환한다. 존재하지 않는 ID는 결과에서 빠진다.
	 */
	List<MentionTargetResult> findSummaries(Collection<Long> targetIds);
}
