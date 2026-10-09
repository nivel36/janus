/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
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
 * <p>
 * This service handles scenarios where an employee clocks out without having a
 * corresponding clock-in event. It provides operations to either resolve the
 * event by creating a valid {@link TimeLog} entry or invalidate the event when
 * it is considered incorrect or unusable.
 * </p>
 * <p>
 * Resolution and invalidation persist the final state transactionally. Lookup
 * returns an existing event without changing it.
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
	 * @param  clockOutWithoutClockInEventRepository repository used to persist
	 *                                               {@link ClockOutWithoutClockInEvent}
	 *                                               entities. Must not be
	 *                                               {@code null}.
	 * @param  timeLogService                        service used to create
	 *                                               {@link TimeLog} records. Must
	 *                                               not be {@code null}.
	 * @throws NullPointerException                  if any dependency is
	 *                                               {@code null}
	 */
	public ClockOutWithoutClockInEventService(
		final ClockOutWithoutClockInEventRepository clockOutWithoutClockInEventRepository,
		final TimeLogService timeLogService) {
		this.clockOutWithoutClockInEventRepository = Objects.requireNonNull(
				clockOutWithoutClockInEventRepository,
				"clockOutWithoutClockInEventRepository can't be null");
		this.timeLogService = Objects.requireNonNull(timeLogService, "timeLogService can't be null");
	}

	/**
	 * Resolves an event by creating and associating a closed time log.
	 * <p>
	 * The supplied entry and the event's exit are subject to
	 * {@link TimeLogService#createTimeLog(Employee, Worksite, Instant, Instant)}
	 * validation. A successful operation finalizes the event; a final event cannot
	 * be resolved again. A present reason is stored as supplied.
	 *
	 * @param  clockOutWithoutClockInEvent            the event to resolve; must not
	 *                                                be {@code null}
	 * @param  entryTime                              the proposed clock-in instant;
	 *                                                must not be {@code null}
	 * @param  reason                                 the optional explanation; the
	 *                                                optional must not be
	 *                                                {@code null}
	 * @return                                        the resolved and persisted
	 *                                                event
	 * @throws NullPointerException                   if any argument is
	 *                                                {@code null}
	 * @throws EventAlreadyFinalizedException         if the event is already final
	 * @throws TimeLogModificationNotAllowedException if either instant is locked or
	 *                                                the employee already has a log
	 *                                                with the proposed entry time
	 * @throws TimeLogFutureTimeException             if a truncated instant is in
	 *                                                the future
	 * @throws TimeLogChronologyException             if truncated exit is not
	 *                                                strictly after entry
	 */
	@Transactional
	public ClockOutWithoutClockInEvent resolve(
			final ClockOutWithoutClockInEvent clockOutWithoutClockInEvent,
			final Instant entryTime,
			final Optional<String> reason) {
		Objects.requireNonNull(clockOutWithoutClockInEvent, "clockOutWithoutClockInEvent can't be null");
		Objects.requireNonNull(entryTime, "entryTime can't be null");

		logger.debug("Resolving clockOutWithoutClockInEvent {} at {}", clockOutWithoutClockInEvent, entryTime);
		if (clockOutWithoutClockInEvent.isResolved() || clockOutWithoutClockInEvent.isInvalidated()) {
			throw new EventAlreadyFinalizedException();
		}

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
	 * Invalidates and persists an event, optionally recording a reason.
	 * <p>
	 * Invalidation finalizes the event without creating a time log. A present
	 * reason is stored as supplied.
	 *
	 * @param  clockOutWithoutClockInEvent    the event to invalidate; must not be
	 *                                        {@code null}
	 * @param  reason                         the optional explanation; the optional
	 *                                        must not be {@code null}
	 * @return                                the invalidated and persisted event
	 * @throws NullPointerException           if either argument is {@code null}
	 * @throws EventAlreadyFinalizedException if the event is already final
	 */
	@Transactional
	public ClockOutWithoutClockInEvent invalidate(
			final ClockOutWithoutClockInEvent clockOutWithoutClockInEvent,
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
	 * Returns the event matching the supplied employee, worksite and exact exit
	 * instant.
	 *
	 * @param  employee                  the employee associated with the event;
	 *                                   must not be {@code null}
	 * @param  worksite                  the worksite associated with the event;
	 *                                   must not be {@code null}
	 * @param  exitTime                  the exact exit instant; must not be
	 *                                   {@code null}
	 * @return                           the matching event
	 * @throws NullPointerException      if any argument is {@code null}
	 * @throws ResourceNotFoundException if no event matches
	 */
	@Transactional(readOnly = true)
	public ClockOutWithoutClockInEvent findClockOutWithoutClockInEventByEmployeeAndWorksiteAndExitTime(
			final Employee employee,
			final Worksite worksite,
			final Instant exitTime) {
		Objects.requireNonNull(employee, "employee can't be null");
		Objects.requireNonNull(worksite, "worksite can't be null");
		Objects.requireNonNull(exitTime, "exitTime can't be null");

		logger.debug(
				"Finding clockOutWithoutClockInEvent by  by employee {}, worksite {} and exitTime{}",
				employee,
				worksite,
				exitTime);
		final ClockOutWithoutClockInEvent clockOutWithoutClockInEvent = this.clockOutWithoutClockInEventRepository
				.findByEmployeeAndWorksiteAndExitTime(employee, worksite, exitTime);
		if (clockOutWithoutClockInEvent == null) {
			// We are searching by natural (composite) key. So, if we not found it an
			// exception is thrown.
			throw new ResourceNotFoundException(
					String.format(
							"ClockOutWithoutClockInEvent with  by employee %s, worksite %s and exitTime %s",
							employee,
							worksite,
							exitTime));
		}
		return clockOutWithoutClockInEvent;
	}
}
