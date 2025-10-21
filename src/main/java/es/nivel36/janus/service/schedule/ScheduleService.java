/*
 * Copyright 2025 Abel Ferrer Jiménez
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
package es.nivel36.janus.service.schedule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.nivel36.janus.service.ResourceAlreadyExistsException;
import es.nivel36.janus.service.ResourceNotFoundException;
import es.nivel36.janus.service.employee.Employee;

/**
 * Service class responsible for managing schedules and retrieving time ranges
 * for employees.
 *
 * <p>
 * The {@code ScheduleService} provides methods to interact with schedules and
 * time ranges associated with employees, allowing the system to determine the
 * working hours of an employee on a specific date. This service uses the
 * {@link ScheduleRepository} to perform the necessary database operations.
 * </p>
 */
@Service
public class ScheduleService {

	private static final Logger logger = LoggerFactory.getLogger(ScheduleService.class);

	private final ScheduleRepository scheduleRepository;

	public ScheduleService(final ScheduleRepository scheduleRepository) {
		this.scheduleRepository = Objects.requireNonNull(scheduleRepository, "Schedule repository can't be null");
	}

	/**
	 * Creates and persists a new {@link Schedule}.
	 *
	 * <p>
	 * Associates each {@link ScheduleRule} back to the parent schedule to keep the
	 * bidirectional relationship consistent, then persists the aggregate.
	 * </p>
	 *
	 * @param name  non-@{code null} schedule name
	 * @param code  non-@{code null} unique business code
	 * @param rules to attach can't be @{code null}
	 * @return the persisted {@link Schedule}
	 *
	 * @throws NullPointerException           if any string is @{code null}
	 * @throws ResourceAlreadyExistsException if a schedule with the same code
	 *                                        already exists
	 */
	@Transactional
	public Schedule createSchedule(final String name, final String code, final List<ScheduleRule> rules) {
		Objects.requireNonNull(name, "name can't be null");
		Objects.requireNonNull(name, "code can't be null");
		Objects.requireNonNull(rules, "rules can't be null");
		logger.debug("Creating schedule {}", code);

		final boolean existsByCode = this.scheduleRepository.existsByCode(code);
		if (existsByCode) {
			logger.warn("Unable to create schedule. Code {} already exists", code);
			throw new ResourceAlreadyExistsException("Schedule already exists with code " + code);
		}

		final Schedule schedule = new Schedule();
		schedule.setCode(code);
		schedule.setName(name);
		this.attachScheduleToRules(schedule, rules);
		final Schedule savedSchedule = this.scheduleRepository.save(schedule);
		logger.trace("Schedule {} created successfully", code);
		return savedSchedule;
	}

	/**
	 * Updates an existing {@link Schedule} identified by its primary key.
	 *
	 * <p>
	 * The method replaces the schedule's mutable fields (name and rules) with the
	 * values provided in {@code updatedSchedule}. Rules not present in the new
	 * collection are removed thanks to the {@code orphanRemoval=true} setting in
	 * {@link Schedule#getRules()}.
	 * </p>
	 *
	 * @param code            the code of the schedule to update; must not be
	 *                        {@code null}
	 * @param schedule the schedule containing the new values; must not be
	 *                        {@code null}
	 * @return the updated {@link Schedule}
	 * @throws NullPointerException      if {@code scheduleId} or
	 *                                   {@code updatedSchedule} is {@code null}
	 * @throws ResourceNotFoundException if the schedule does not exist
	 */
	@Transactional
	public Schedule updateSchedule(final String code, final Schedule schedule) {
		Objects.requireNonNull(code, "code can't be null");
		Objects.requireNonNull(schedule, "schedule can't be null");
		logger.debug("Updating schedule {}", code);

		final Schedule persisted = this.findSchedule(code);
		persisted.setName(schedule.getName());
		persisted.getRules().clear();
		final List<ScheduleRule> newRules = schedule.getRules();
		if (newRules != null && !newRules.isEmpty()) {
			this.attachScheduleToRules(persisted, newRules);
			persisted.getRules().addAll(newRules);
		}

		final Schedule updatedSchedule = this.scheduleRepository.save(persisted);
		logger.trace("Schedule {} updated successfully", code);
		return updatedSchedule;
	}

        private void attachScheduleToRules(final Schedule schedule, final List<ScheduleRule> rules) {
                if (rules == null) {
                        return;
                }
                for (final ScheduleRule rule : rules) {
			if (rule != null) {
				rule.setSchedule(schedule);
			}
		}
	}

	/**
	 * Deletes an existing {@link Schedule} by its primary key.
	 * 
	 * Before deletion, it is verified that the schedule has no assigned workers.
	 *
	 * @param schedule the schedule to delete; must not be {@code null}
	 * @throws NullPointerException  if {@code schedule} is {@code null}
	 * @throws IllegalStateException if the schedule has assigned employees and
	 *                               can't be deleted
	 */
	@Transactional
	public void deleteSchedule(final Schedule schedule) {
		Objects.requireNonNull(schedule, "Schedule can't be null");
		logger.debug("Deleting Schedule {}", schedule);

		final boolean inUse = this.scheduleRepository.hasEmployees(schedule);
		if (inUse) {
			throw new IllegalStateException(
					"The schedule " + schedule + " can't be deleted because it has assigned employees");
		}
		this.scheduleRepository.delete(schedule);
	}

	/**
	 * Retrieves a {@link Schedule} by its code, throwing an exception if it does
	 * not exist.
	 *
	 * @param code the unique schedule code; must not be {@code null}
	 * @return the {@link Schedule} with the given code
	 * @throws NullPointerException      if {@code code} is {@code null}
	 * @throws ResourceNotFoundException if the schedule does not exist
	 */
	@Transactional(readOnly = true)
	public Schedule findScheduleByCode(final String code) {
		Objects.requireNonNull(code, "code can't be null");
		logger.debug("Finding schedule by code {}", code);

                return this.findSchedule(code);
        }

        /**
         * Retrieves all {@link Schedule} aggregates available in the repository.
         *
         * @return immutable list containing every persisted schedule
         */
        @Transactional(readOnly = true)
        public List<Schedule> findAllSchedules() {
                logger.debug("Listing all schedules");

                final Iterable<Schedule> schedules = this.scheduleRepository.findAll();
                return StreamSupport.stream(schedules.spliterator(), false).toList();
        }

	private Schedule findSchedule(final String code) {
		final Schedule schedule = this.scheduleRepository.findByCode(code);
		if (schedule == null) {
			logger.warn("No schedule found with code {}", code);
			throw new ResourceNotFoundException("No schedule found with code " + code);
		}
		return schedule;
	}

	/**
	 * Finds the {@link TimeRange} for a given {@link Employee} on a specific
	 * {@link LocalDate}.
	 *
	 * <p>
	 * This method searches for the time range that applies to the employee on the
	 * given date. If no time range is defined for that day (e.g., a Sunday or a
	 * holiday), it returns an empty {@link Optional}.
	 * </p>
	 *
	 * @param employee the employee whose time range is to be found. Cannot be
	 *                 {@code null}.
	 * @param date     the date for which the time range is to be found. Cannot be
	 *                 {@code null}.
	 * @return an {@code Optional} containing the {@link TimeRange} if one is
	 *         defined, or an empty {@code Optional} if no time range exists.
	 * @throws NullPointerException if either {@code employee} or {@code date} is
	 *                              {@code null}.
	 */
	public Optional<TimeRange> findTimeRangeForEmployeeByDate(final Employee employee, final LocalDate date) {
		Objects.requireNonNull(employee, "Employee can't be null");
		Objects.requireNonNull(date, "Date can't be null");
		logger.debug("Finding time range for employee {} by date {}", employee, date);

		final DayOfWeek dayOfWeek = date.getDayOfWeek();
		return this.scheduleRepository.findTimeRangeForDate(employee, date, dayOfWeek);
	}
}
