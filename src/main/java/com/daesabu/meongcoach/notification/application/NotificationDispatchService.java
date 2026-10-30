package com.daesabu.meongcoach.notification.application;

import com.daesabu.meongcoach.notification.application.provided.NotificationDispatchResult;
import com.daesabu.meongcoach.notification.application.provided.NotificationDispatcher;
import com.daesabu.meongcoach.notification.application.provided.NotificationTargetFinder;
import com.daesabu.meongcoach.notification.application.provided.NotificationTargetResult;
import com.daesabu.meongcoach.notification.application.provided.dto.NotificationDispatchRequest;
import com.daesabu.meongcoach.notification.application.required.DeviceInstallationRepository;
import com.daesabu.meongcoach.notification.application.required.NotificationDispatchRepository;
import com.daesabu.meongcoach.notification.application.required.NotificationSendResult;
import com.daesabu.meongcoach.notification.application.required.NotificationSender;
import com.daesabu.meongcoach.notification.domain.NotificationDispatch;
import com.daesabu.meongcoach.notification.domain.NotificationHour;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Validated
public class NotificationDispatchService implements NotificationDispatcher {
	private final NotificationDispatchRepository dispatches;
	private final NotificationTargetFinder targets;
	private final DeviceInstallationRepository installations;
	private final NotificationSender sender;

	@Override
	@Transactional
	public NotificationDispatchResult dispatch(NotificationDispatchRequest request) {
		NotificationHour.validate(request.hour());
		if (!dispatches.tryLock(request.key()) || dispatches.existsByDispatchKey(request.key())) {
			return new NotificationDispatchResult(0, true);
		}
		List<String> fids = targets.findTargets(request.hour()).stream()
				.map(NotificationTargetResult::fid).distinct().toList();
		NotificationSendResult result = send(fids, request.hour());
		deleteInvalidInstallations(result.invalidFids());
		if (result.failure() != null) {
			throw result.failure();
		}
		NotificationDispatch dispatch = NotificationDispatch.complete(request.key(), Instant.now());
		dispatches.save(dispatch);
		return new NotificationDispatchResult(fids.size(), false);
	}

	private NotificationSendResult send(List<String> fids, int hour) {
		if (fids.isEmpty()) {
			List<String> invalidFids = List.of();
			return new NotificationSendResult(invalidFids, null);
		}
		return sender.send(fids, hour);
	}

	private void deleteInvalidInstallations(List<String> invalidFids) {
		if (invalidFids.isEmpty()) {
			return;
		}
		installations.deleteAllByFidIn(invalidFids);
	}
}
