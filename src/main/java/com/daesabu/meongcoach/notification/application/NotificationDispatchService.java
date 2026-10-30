package com.daesabu.meongcoach.notification.application;

import com.daesabu.meongcoach.notification.application.provided.NotificationDispatchResult;
import com.daesabu.meongcoach.notification.application.provided.NotificationDispatcher;
import com.daesabu.meongcoach.notification.application.provided.NotificationRegister;
import com.daesabu.meongcoach.notification.application.provided.NotificationTargetFinder;
import com.daesabu.meongcoach.notification.application.provided.NotificationTargetResult;
import com.daesabu.meongcoach.notification.application.provided.dto.DeviceInstallationDeleteRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.NotificationDispatchRequest;
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
	private final NotificationRegister register;
	private final NotificationSender sender;

	@Override
	@Transactional
	public NotificationDispatchResult dispatch(NotificationDispatchRequest request) {
		NotificationHour.validate(request.hour());
		if (!dispatches.tryLock(request.key()) || dispatches.existsById(request.key())) {
			return new NotificationDispatchResult(0, true);
		}
		NotificationDispatchResult result = send(request.hour());
		NotificationDispatch dispatch = NotificationDispatch.complete(request.key(), Instant.now());
		dispatches.save(dispatch);
		return result;
	}

	private NotificationDispatchResult send(int hour) {
		List<String> fids = targets.findTargets(hour).stream()
				.map(NotificationTargetResult::fid).distinct().toList();
		if (fids.isEmpty()) {
			return new NotificationDispatchResult(0, false);
		}
		NotificationSendResult result = sender.send(fids, hour);
		if (!result.invalidFids().isEmpty()) {
			register.deleteDeviceInstallations(new DeviceInstallationDeleteRequest(result.invalidFids()));
		}
		if (result.failure() != null) {
			throw result.failure();
		}
		return new NotificationDispatchResult(fids.size(), false);
	}
}
