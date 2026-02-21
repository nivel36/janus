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
package es.nivel36.janus.service.schedule;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.nivel36.janus.service.ResourceAlreadyExistsException;
import es.nivel36.janus.service.ResourceNotFoundException;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.util.Strings;

/**
 * Service class responsible for managing {@link Schedule} aggregates and
 * retrieving {@link TimeRange time ranges} for employees.
 *
 * <p>
 * {@code ScheduleService} provides operations to create, update, delete and
 * query schedules, as well as to determine the working hours of an
 * {@link Employee} on a specific {@link LocalDate}. It acts as the application
 * layer façade for schedule-related use cases.
 * </p>
 *
 * <p>
 * Persistence operations are delegated to {@link ScheduleRepository}. All
 * modifying operations are executed within a transactional context.
 * </p>
 */
@Service
public class ScheduleService {

	private static final Logger logger = LoggerFactory.getLogger(ScheduleService.class);

	/**
	 * Repository used to persist and retrieve {@link Schedule} aggregates.
	 */
	private final ScheduleRepository scheduleRepository;

	/**
	 * Creates a new {@code ScheduleService} instance.
	 *
	 * @param scheduleRepository repository used to manage schedules; can't be
	 *                           {@code null}
	 * @throws NullPointerException if {@code scheduleRepository} is {@code null}
	 */
	public ScheduleService(final ScheduleRepository scheduleRepository) {
		this.scheduleRepository = Objects.requireNonNull(scheduleRepository, "Schedule repository can't be null");
	}

	/**
	 * Creates and persists a new {@link Schedule}.
	 *
	 * <p>
	 * The schedule is created using the provided code, name and rule definitions.
	 * The schedule code must be unique in the system.
	 * </p>
	 *
	 * @param code  unique schedule code; can't be {@code null} or blank
	 * @param name  human-readable schedule name; can't be {@code null} or blank
	 * @param rules rule definitions associated with the schedule; can't be
	 *              {@code null}
	 * @return the persisted {@link Schedule}
	 * @throws NullPointerException           if {@code rules} is {@code null}
	 * @throws IllegalArgumentException       if {@code code} or {@code name} is
	 *                                        blank
	 * @throws ResourceAlreadyExistsException if a schedule with the same code
	 *                                        already exists
	 */
	@Transactional
	public Schedule createSchedule(final String code, final String name, final List<ScheduleRuleDefinition> rules) {
		Strings.requireNonBlank(code, "code can't be null or blank");
		Strings.requireNonBlank(name, "name can't be null or blank");
		Objects.requireNonNull(rules, "rules can't be null");
		logger.debug("Creating schedule {}", code);
		this.assertScheduleCodeIsUnique(code);

		final Schedule schedule = this.buildSchedule(code, name, rules);
		final Schedule savedSchedule = this.scheduleRepository.save(schedule);
		logger.trace("Schedule {} created successfully", code);
		return savedSchedule;
	}

	private void assertScheduleCodeIsUnique(final String code) {
		final boolean existsByCode = this.scheduleRepository.existsByCode(code);
		if (existsByCode) {
			throw new ResourceAlreadyExistsException("Schedule already exists with code " + code);
		}
	}

	private Schedule buildSchedule(final String code, final String name, final List<ScheduleRuleDefinition> rules) {
		final Schedule schedule = new Schedule(code, name);
		for (final ScheduleRuleDefinition ruleDefinition : rules) {
			final ScheduleRule rule = this.buildRule(schedule, ruleDefinition);
			schedule.addRule(rule);
		}
		return schedule;
	}

	private ScheduleRule buildRule(final Schedule schedule, final ScheduleRuleDefinition ruleDefinition) {
		final ScheduleRule rule = new ScheduleRule(ruleDefinition.name(), schedule);
		rule.setActivePeriod(ruleDefinition.startDate(), ruleDefinition.endDate());

		final List<ScheduleRuleTimeRangeDefinition> dayOfWeekDefinitionRanges = ruleDefinition.dayOfWeekRanges();
		for (final ScheduleRuleTimeRangeDefinition timeRangeDefinition : dayOfWeekDefinitionRanges) {
			final DayOfWeekTimeRange dayOfWeekTimeRange = this.buildDayOfWeekTimeRange(rule, timeRangeDefinition);
			rule.addRange(dayOfWeekTimeRange);
		}
		return rule;
	}

	private DayOfWeekTimeRange buildDayOfWeekTimeRange(final ScheduleRule rule,
			final ScheduleRuleTimeRangeDefinition timeRangeDefinition) {
		final DayOfWeek dayOfWeek = timeRangeDefinition.dayOfWeek();
		final LocalTime startTime = timeRangeDefinition.startTime();
		final LocalTime endTime = timeRangeDefinition.endTime();
		final TimeRange timeRange = new TimeRange(startTime, endTime);
		final Duration effectiveWorkHours = timeRangeDefinition.effectiveWorkHours();
		return new DayOfWeekTimeRange(rule, dayOfWeek, timeRange, effectiveWorkHours);
	}

	/**
	 * Updates an existing {@link Schedule} identified by its code.
	 *
	 * <p>
	 * The schedule name and rule set are replaced by the provided values. Existing
	 * rules not present in the new definition are removed due to
	 * {@code orphanRemoval=true} configuration in {@link Schedule#getRules()}.
	 * </p>
	 *
	 * @param code  code of the schedule to update; can't be {@code null} or blank
	 * @param name  new schedule name; can't be {@code null} or blank
	 * @param rules new rule definitions; can't be {@code null}
	 * @return the updated {@link Schedule}
	 * @throws NullPointerException      if {@code rules} is {@code null}
	 * @throws IllegalArgumentException  if {@code code} or {@code name} is blank
	 * @throws ResourceNotFoundException if the schedule does not exist
	 */
	@Transactional
	public Schedule updateSchedule(final String code, final String name, final List<ScheduleRuleDefinition> rules) {
		Strings.requireNonBlank(code, "code can't be null or blank");
		Strings.requireNonBlank(name, "name can't be null or blank");
		Objects.requireNonNull(rules, "rules can't be null");
		logger.debug("Updating schedule {}", code);

		final Schedule persisted = this.findSchedule(code);
		persisted.setName(name);
		persisted.clearRules();

		for (final ScheduleRuleDefinition ruleDefinition : rules) {
			final ScheduleRule rule = this.buildRule(persisted, ruleDefinition);
			persisted.addRule(rule);
		}
		return persisted;
	}

	/**
	 * Deletes an existing {@link Schedule}.
	 *
	 * <p>
	 * A schedule can only be deleted if it has no employees assigned.
	 * </p>
	 *
	 * @param schedule schedule to delete; can't be {@code null}
	 * @throws NullPointerException  if {@code schedule} is {@code null}
	 * @throws IllegalStateException if the schedule has assigned employees
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
	 * Retrieves a {@link Schedule} by its unique code.
	 *
	 * @param code unique schedule code; can't be {@code null}
	 * @return the {@link Schedule} associated with the given code
	 * @throws NullPointerException      if {@code code} is {@code null}
	 * @throws ResourceNotFoundException if no schedule exists with the given code
	 */
	@Transactional(readOnly = true)
	public Schedule findScheduleByCode(final String code) {
		Objects.requireNonNull(code, "code can't be null");
		logger.debug("Finding schedule by code {}", code);

		return this.findSchedule(code);
	}

	private Schedule findSchedule(final String code) {
		final Schedule schedule = this.scheduleRepository.findByCode(code);
		if (schedule == null) {
			throw new ResourceNotFoundException("No schedule found with code " + code);
		}
		return schedule;
	}

	/**
	 * Retrieves all persisted {@link Schedule} instances.
	 *
	 * @return a list containing all schedules
	 */
	@Transactional(readOnly = true)
	public List<Schedule> findAllSchedules() {
		logger.debug("Listing all schedules");
		return this.scheduleRepository.findAll();
	}

	/**
	 * Finds the {@link TimeRange} applicable to an {@link Employee} on a given
	 * {@link LocalDate}.
	 *
	 * <p>
	 * If no time range applies for the given date (for example, non-working days),
	 * an empty {@link Optional} is returned.
	 * </p>
	 *
	 * @param employee employee whose working time is requested; can't be
	 *                 {@code null}
	 * @param date     date to evaluate; can't be {@code null}
	 * @return an {@link Optional} containing the applicable {@link TimeRange}, or
	 *         an empty {@code Optional} if none applies
	 * @throws NullPointerException if {@code employee} or {@code date} is
	 *                              {@code null}
	 */
	@Transactional(readOnly = true)
	public Optional<TimeRange> findTimeRangeForEmployeeByDate(final Employee employee, final LocalDate date) {
		Objects.requireNonNull(employee, "Employee can't be null");
		Objects.requireNonNull(date, "Date can't be null");
		logger.debug("Finding time range for employee {} by date {}", employee, date);

		final DayOfWeek dayOfWeek = date.getDayOfWeek();
		return this.scheduleRepository.findTimeRangeForDate(employee, date, dayOfWeek);
	}
}
