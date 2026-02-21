/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package es.nivel36.janus.service.timelog;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.nivel36.janus.service.ResourceNotFoundException;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.worksite.Worksite;

/**
 * Service responsible for managing {@link ClockOutWithoutClockInEvent}
 * resolution and invalidation.
 *
 * <p>
 * This service handles scenarios where an employee clocks out without having a
 * corresponding clock-in event. It provides operations to either resolve the
 * event by creating a valid {@link TimeLog} entry or invalidate the event when
 * it is considered incorrect or unusable.
 * </p>
 *
 * <p>
 * All operations are transactional and persist the updated state of the
 * {@link ClockOutWithoutClockInEvent} using
 * {@link ClockOutWithoutClockInEventRepository}.
 * </p>
 */
@Service
public class ClockOutWithoutClockInEventService {

	private static final Logger logger = LoggerFactory.getLogger(ClockOutWithoutClockInEventService.class);

	private final ClockOutWithoutClockInEventRepository clockOutWithoutClockInEventRepository;
	private final TimeLogService timeLogService;

	/**
	 * Creates a new {@code ClockOutWithoutClockInEventService}.
	 *
	 * @param clockOutWithoutClockInEventRepository repository used to persist
	 *                                              {@link ClockOutWithoutClockInEvent}
	 *                                              entities. Can't be {@code null}.
	 * @param timeLogService                        service used to create
	 *                                              {@link TimeLog} records. Can't
	 *                                              be {@code null}.
	 * @throws NullPointerException if any dependency is {@code null}
	 */
	public ClockOutWithoutClockInEventService(
			final ClockOutWithoutClockInEventRepository clockOutWithoutClockInEventRepository,
			final TimeLogService timeLogService) {
		this.clockOutWithoutClockInEventRepository = Objects.requireNonNull(clockOutWithoutClockInEventRepository,
				"clockOutWithoutClockInEventRepository can't be null");
		this.timeLogService = Objects.requireNonNull(timeLogService, "timeLogService can't be null");
	}

	/**
	 * Resolves the specified {@link ClockOutWithoutClockInEvent} by creating a
	 * corresponding {@link TimeLog}.
	 *
	 * <p>
	 * The event is marked as resolved and associated with the newly created
	 * {@link TimeLog}. An optional reason can be provided to justify the
	 * resolution.
	 * </p>
	 *
	 * @param clockOutWithoutClockInEvent event to be resolved. Can't be
	 *                                    {@code null}.
	 * @param entryTime                   entry time to be used when creating the
	 *                                    {@link TimeLog}. Can't be {@code null}.
	 * @param reason                      optional reason explaining the resolution.
	 *                                    May be {@code empty}.
	 * @return the resolved and persisted {@link ClockOutWithoutClockInEvent}.
	 */
	@Transactional
	public ClockOutWithoutClockInEvent resolve(final ClockOutWithoutClockInEvent clockOutWithoutClockInEvent,
			final Instant entryTime, final Optional<String> reason) {
		Objects.requireNonNull(clockOutWithoutClockInEvent, "clockOutWithoutClockInEvent can't be null");
		Objects.requireNonNull(entryTime, "entryTime can't be null");

		logger.debug("Resolving clockOutWithoutClockInEvent {} at {}", clockOutWithoutClockInEvent, entryTime);

		final Employee employee = clockOutWithoutClockInEvent.getEmployee();
		final Worksite worksite = clockOutWithoutClockInEvent.getWorksite();
		final Instant exitTime = clockOutWithoutClockInEvent.getExitTime();
		final TimeLog timeLog = this.timeLogService.createTimeLog(employee, worksite, entryTime, exitTime);
		if (reason.isPresent()) {
			clockOutWithoutClockInEvent.resolve(timeLog, reason.get());
		} else {
			clockOutWithoutClockInEvent.resolve(timeLog);
		}
		return this.clockOutWithoutClockInEventRepository.save(clockOutWithoutClockInEvent);
	}

	/**
	 * Invalidates the specified {@link ClockOutWithoutClockInEvent}.
	 *
	 * <p>
	 * The event is marked as invalid and persisted. An optional reason can be
	 * provided to explain why the event has been invalidated.
	 * </p>
	 *
	 * @param clockOutWithoutClockInEvent event to be invalidated. Can't be
	 *                                    {@code null}.
	 * @param reason                      optional reason explaining the
	 *                                    invalidation. May be {@code empty}.
	 * @return the invalidated and persisted {@link ClockOutWithoutClockInEvent}.
	 */
	@Transactional
	public ClockOutWithoutClockInEvent invalidate(final ClockOutWithoutClockInEvent clockOutWithoutClockInEvent,
			final Optional<String> reason) {
		Objects.requireNonNull(clockOutWithoutClockInEvent, "clockOutWithoutClockInEvent can't be null");

		logger.debug("Invalidating clockOutWithoutClockInEvent {} ", clockOutWithoutClockInEvent);

		if (reason.isPresent()) {
			clockOutWithoutClockInEvent.invalidate(reason.get());
		} else {
			clockOutWithoutClockInEvent.invalidate();
		}
		return this.clockOutWithoutClockInEventRepository.save(clockOutWithoutClockInEvent);
	}

	/**
	 * Retrieves a {@link ClockOutWithoutClockInEvent} associated with the given
	 * {@link Employee}, {@link Worksite}, and exit {@link Instant}.
	 *
	 * @param employee the employee associated with the event. Can't be
	 *                 {@code null}.
	 * @param worksite the worksite where the event occurred. Can't be {@code null}.
	 * @param exitTime the exit time of the event. Can't be {@code null}.
	 * @return the matching {@link ClockOutWithoutClockInEvent}, or {@code null} if
	 *         no event exists for the specified employee, worksite, and exit time.
	 * @throws NullPointerException      if any of the parameters is {@code null}.
	 * @throws ResourceNotFoundException if the event is not found.
	 */
	@Transactional(readOnly = true)
	public ClockOutWithoutClockInEvent findClockOutWithoutClockInEventByEmployeeAndWorksiteAndExitTime(
			Employee employee, Worksite worksite, Instant exitTime) {
		Objects.requireNonNull(employee, "employee can't be null");
		Objects.requireNonNull(worksite, "worksite can't be null");
		Objects.requireNonNull(exitTime, "exitTime can't be null");

		logger.debug("Finding clockOutWithoutClockInEvent by  by employee {}, worksite {} and exitTime{}", employee,
				worksite, exitTime);
		final ClockOutWithoutClockInEvent clockOutWithoutClockInEvent = this.clockOutWithoutClockInEventRepository
				.findByEmployeeAndWorksiteAndExitTime(employee, worksite, exitTime);
		if (clockOutWithoutClockInEvent == null) {
			// We are searching by natural (composite) key. So, if we not found it an
			// exception is thrown.
			throw new ResourceNotFoundException(
					String.format("ClockOutWithoutClockInEvent with  by employee %s, worksite %s and exitTime %s",
							employee, worksite, exitTime));
		}
		return clockOutWithoutClockInEvent;
	}
}
