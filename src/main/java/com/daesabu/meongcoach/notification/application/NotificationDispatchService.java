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
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Validated
public class NotificationDispatchService implements NotificationDispatcher {
	private final NotificationDispatchRepository dispatches;
	private final TransactionTemplate transactions;
	private final NotificationTargetFinder targets;
	private final NotificationRegister register;
	private final NotificationSender sender;

	@Override
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public NotificationDispatchResult dispatch(NotificationDispatchRequest request) {
		NotificationHour.validate(request.hour());
		if (!start(request.key())) {
			return new NotificationDispatchResult(0, 0, 0, true);
		}
		NotificationDispatchResult result;
		try {
			result = send(request.hour());
		} catch (RuntimeException failure) {
			try {
				transactions.executeWithoutResult(status -> dispatches.getReferenceById(request.key()).fail());
			} catch (RuntimeException stateFailure) {
				failure.addSuppressed(stateFailure);
			}
			throw failure;
		}
		transactions.executeWithoutResult(status -> dispatches.getReferenceById(request.key()).complete());
		return result;
	}

	private boolean start(String key) {
		try {
			Boolean started = transactions.execute(status -> {
				Optional<NotificationDispatch> existing = dispatches.findById(key);
				if (existing.isPresent()) {
					return existing.get().retry();
				}
				NotificationDispatch dispatch = NotificationDispatch.start(key, Instant.now());
				dispatches.saveAndFlush(dispatch);
				return true;
			});
			return Boolean.TRUE.equals(started);
		} catch (DataIntegrityViolationException | OptimisticLockingFailureException conflict) {
			return false;
		}
	}

	private NotificationDispatchResult send(int hour) {
		List<String> fids = targets.findTargets(hour).stream()
				.map(NotificationTargetResult::fid).distinct().toList();
		if (fids.isEmpty()) {
			return new NotificationDispatchResult(0, 0, 0, false);
		}
		NotificationSendResult result = sender.send(fids, hour);
		if (!result.invalidFids().isEmpty()) {
			register.deleteDeviceInstallations(new DeviceInstallationDeleteRequest(result.invalidFids()));
		}
		if (result.failure() != null) {
			throw result.failure();
		}
		return new NotificationDispatchResult(fids.size(), result.invalidFids().size(), result.failed(), false);
	}
}
