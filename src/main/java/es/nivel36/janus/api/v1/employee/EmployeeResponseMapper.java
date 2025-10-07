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
package es.nivel36.janus.api.v1.employee;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import es.nivel36.janus.api.Mapper;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.schedule.Schedule;
import es.nivel36.janus.service.worksite.Worksite;

/**
 * Maps {@link Employee} entities into {@link EmployeeResponse} DTOs.
 */
@Component
public class EmployeeResponseMapper implements Mapper<Employee, EmployeeResponse> {

        @Override
        public EmployeeResponse map(final Employee employee) {
                if (employee == null) {
                        return null;
                }

                final Long scheduleId = extractScheduleId(employee.getSchedule());
                final Set<String> worksiteCodes = extractWorksiteCodes(employee.getWorksites());

                return new EmployeeResponse(employee.getId(), employee.getName(), employee.getSurname(), employee.getEmail(),
                                scheduleId, worksiteCodes);
        }

        private Long extractScheduleId(final Schedule schedule) {
                if (schedule == null) {
                        return null;
                }
                return schedule.getId();
        }

        private Set<String> extractWorksiteCodes(final Set<Worksite> worksites) {
                if ((worksites == null) || worksites.isEmpty()) {
                        return Set.of();
                }

                final LinkedHashSet<String> codes = worksites.stream() //
                                .filter(Objects::nonNull) //
                                .map(Worksite::getCode) //
                                .filter(Objects::nonNull) //
                                .collect(Collectors.toCollection(LinkedHashSet::new));

                return Collections.unmodifiableSet(codes);
        }
}
