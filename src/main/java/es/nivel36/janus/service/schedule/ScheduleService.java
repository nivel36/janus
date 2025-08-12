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
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
