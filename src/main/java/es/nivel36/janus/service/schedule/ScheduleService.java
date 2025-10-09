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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
	 * Creates a new {@link Schedule} and persists it using the
	 * {@link ScheduleRepository}.
	 *
	 * <p>
	 * All {@link ScheduleRule} instances contained in the schedule are associated
	 * back to the parent schedule before persistence to maintain the bidirectional
	 * relationship.
	 * </p>
	 *
	 * @param schedule the schedule to create; must not be {@code null}
	 * @return the persisted {@link Schedule}
	 * @throws NullPointerException if {@code schedule} is {@code null}
	 */
	@Transactional
	public Schedule createSchedule(final Schedule schedule) {
		Objects.requireNonNull(schedule, "Schedule can't be null");
		logger.debug("Creating Schedule {}", schedule.getName());

		this.attachScheduleToRules(schedule, schedule.getRules());
		return this.scheduleRepository.save(schedule);
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
	 * @param scheduleId      the primary key of the schedule to update; must not be
	 *                        {@code null}
	 * @param updatedSchedule the schedule containing the new values; must not be
	 *                        {@code null}
	 * @return the updated {@link Schedule}
	 * @throws NullPointerException      if {@code scheduleId} or
	 *                                   {@code updatedSchedule} is {@code null}
	 * @throws ResourceNotFoundException if the schedule does not exist
	 */
	@Transactional
	public Schedule updateSchedule(final Long scheduleId, final Schedule updatedSchedule) {
		Objects.requireNonNull(scheduleId, "Schedule id can't be null");
		Objects.requireNonNull(updatedSchedule, "Updated schedule can't be null");
		logger.debug("Updating Schedule {}", scheduleId);

		final Schedule persisted = this.scheduleRepository.findById(scheduleId)
				.orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id " + scheduleId));

		persisted.setName(updatedSchedule.getName());
		persisted.getRules().clear();

		final List<ScheduleRule> newRules = updatedSchedule.getRules();
		if (newRules != null && !newRules.isEmpty()) {
			this.attachScheduleToRules(persisted, newRules);
			persisted.getRules().addAll(newRules);
		}

		return this.scheduleRepository.save(persisted);
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
	 * Retrieves a {@link Schedule} by its primary key.
	 *
	 * @param scheduleId the identifier of the schedule; must not be {@code null}
	 * @return the {@link Schedule} found
	 * @throws NullPointerException      if {@code scheduleId} is {@code null}
	 * @throws ResourceNotFoundException if the schedule does not exist
	 */
	@Transactional(readOnly = true)
	public Schedule findScheduleById(final Long scheduleId) {
		Objects.requireNonNull(scheduleId, "Schedule id can't be null");
		logger.debug("Finding Schedule {}", scheduleId);

		return this.scheduleRepository.findById(scheduleId)
				.orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id " + scheduleId));
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

	/**
	 * Adds the provided {@link ScheduleRule} to the {@link Schedule}.
	 *
	 * @param schedule     the schedule to update; must not be {@code null}
	 * @param scheduleRule the rule to add; must not be {@code null}
	 * @return the added {@link ScheduleRule}
	 * @throws NullPointerException      if {@code schedule} or {@code scheduleRule}
	 *                                   is {@code null}
	 * @throws ResourceNotFoundException if the schedule does not exist
	 */
	@Transactional
	public ScheduleRule addRuleToSchedule(final Schedule schedule, final ScheduleRule scheduleRule) {
		Objects.requireNonNull(schedule, "Schedule can't be null");
		Objects.requireNonNull(scheduleRule, "Schedule rule can't be null");

		logger.debug("Adding rule {} to schedule {}", scheduleRule, schedule);

		scheduleRule.setSchedule(schedule);
		schedule.getRules().add(scheduleRule);
		this.scheduleRepository.save(schedule);

		return scheduleRule;
	}

	/**
	 * Removes the {@link ScheduleRule} from the {@link Schedule} .
	 *
	 * <p>
	 * If the rule cannot be found within the schedule, a
	 * {@link ResourceNotFoundException} is thrown. Orphan removal on
	 * {@link Schedule#getRules()} ensures the rule is deleted once it is detached
	 * from the collection.
	 * </p>
	 *
	 * @param schedule     the schedule; must not be {@code null}
	 * @param scheduleRule the rule to remove; must not be {@code null}
	 * @throws NullPointerException      if {@code schedule} or {@code scheduleRule}
	 *                                   is {@code null}
	 * @throws ResourceNotFoundException if the rule within that schedule does not
	 *                                   exist
	 */
	@Transactional
	public void removeRuleFromSchedule(final Schedule schedule, final ScheduleRule scheduleRule) {
		Objects.requireNonNull(schedule, "Schedule can't be null");
		Objects.requireNonNull(scheduleRule, "Schedule rule can't be null");

		logger.debug("Removing rule {} from Schedule {}", scheduleRule, schedule);

		final boolean removed = schedule.getRules().removeIf(scheduleRule::equals);

		if (!removed) {
			throw new ResourceNotFoundException("Schedule rule " + scheduleRule + "not found in schedule " + schedule);
		}

		this.scheduleRepository.save(schedule);
	}
}
