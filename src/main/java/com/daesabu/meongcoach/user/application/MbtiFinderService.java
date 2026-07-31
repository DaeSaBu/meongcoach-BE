package com.daesabu.meongcoach.user.application;

import com.daesabu.meongcoach.user.application.provided.MbtiFinder;
import com.daesabu.meongcoach.user.domain.Mbti;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 사람 MBTI 목록을 조회한다.
 */
@Service
public class MbtiFinderService implements MbtiFinder {

	@Override
	public List<String> findAllCodes() {
		return Arrays.stream(Mbti.values())
				.map(Enum::name)
				.toList();
	}
}
