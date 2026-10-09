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
 * distributed under this License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package es.nivel36.janus.service.workshift;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import es.nivel36.janus.service.timelog.TimeLog;
import es.nivel36.janus.service.timelog.TimeLogs;
import es.nivel36.janus.service.workshift.UnscheduledShiftStrategy.PauseInfo;

/**
 * Selects the segment through the log preceding the first supplied long pause.
 * <p>
 * Selection uses the pause's {@code before} log in the supplied collection. The
 * required date does not affect segment selection.
 */
final class LeftSegmentExtractor implements TimeLogsExtractor {

	/**
	 * Returns the segment through the log preceding the first pause.
	 *
	 * @param  date                  the required shift date; does not affect
	 *                               selection
	 * @param  timeLogs              the non-null, non-empty logs to split
	 * @param  pauses                the non-null, non-empty pauses in chronological
	 *                               order
	 * @return                       the selected segment
	 * @throws NullPointerException  if an argument or the first pause is
	 *                               {@code null}
	 * @throws IllegalStateException if logs or pauses are empty or the first
	 *                               pause's {@code before} log is absent from
	 *                               {@code timeLogs}
	 */
	@Override
	public TimeLogs extract(final LocalDate date, final TimeLogs timeLogs, final List<PauseInfo> pauses) {
		Objects.requireNonNull(date, "date must not be null");
		Objects.requireNonNull(timeLogs, "timeLogs must not be null");
		Objects.requireNonNull(pauses, "pauses must not be null");

		if (pauses.isEmpty()) {
			throw new IllegalStateException("At least one pause is required");
		}
		if (timeLogs.isEmpty()) {
			throw new IllegalStateException("At least one time log is required");
		}

		final PauseInfo firstPause = pauses.getFirst();
		final TimeLog cutAt = firstPause.before();

		final int toIndex = timeLogs.indexOf(cutAt);
		if (toIndex < 0) {
			throw new IllegalStateException("Pause 'before' log not found in timeLogs");
		}
		return timeLogs.slice(0, toIndex + 1);
	}
}
