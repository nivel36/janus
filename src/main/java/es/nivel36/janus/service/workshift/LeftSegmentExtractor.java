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
import es.nivel36.janus.service.workshift.UnscheduledShiftStrategy.PauseInfo;

final class LeftSegmentExtractor implements TimeLogsExtractor {

	@Override
	public List<TimeLog> extract(final LocalDate date, final List<TimeLog> timeLogs, final List<PauseInfo> pauses) {
		Objects.requireNonNull(date, "date must not be null");
		Objects.requireNonNull(timeLogs, "timeLogs must not be null");
		Objects.requireNonNull(pauses, "pauses must not be null");
		if (pauses.isEmpty()) {
			throw new IllegalArgumentException("At least one pause is required");
		}
		if (timeLogs.isEmpty()) {
			throw new IllegalArgumentException("At least one time log is required");
		}
		final PauseInfo first = pauses.getFirst();
		final int toIndex = first.index() + 1;
		final List<TimeLog> subList = timeLogs.subList(0, toIndex);
		return new ArrayList<>(subList);
	}
}
