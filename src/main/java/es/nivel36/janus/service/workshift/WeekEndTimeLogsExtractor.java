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
 * distributed under this License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package es.nivel36.janus.service.workshift;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import es.nivel36.janus.service.timelog.TimeLog;
import es.nivel36.janus.service.workshift.WorkShiftService.PauseInfo;

final class WeekEndTimeLogsExtractor implements TimeLogsExtractor {

	@Override
	public List<TimeLog> extract(LocalDate date, List<TimeLog> timeLogs, List<PauseInfo> pauses) {
		Objects.requireNonNull(date, "Date must not be null");
                Objects.requireNonNull(timeLogs, "Time logs must not be null");
		Objects.requireNonNull(pauses, "Pauses must not be null");
		return new ArrayList<>(timeLogs.subList(0, pauses.getFirst().index + 1));
	}
}
