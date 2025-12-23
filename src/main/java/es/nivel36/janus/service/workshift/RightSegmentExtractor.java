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
import java.util.List;
import java.util.Objects;

import es.nivel36.janus.service.timelog.TimeLog;
import es.nivel36.janus.service.timelog.TimeLogs;
import es.nivel36.janus.service.workshift.UnscheduledShiftStrategy.PauseInfo;

/**
 * Extractor implementation that returns the right-hand segment of a list of
 * {@link TimeLog TimeLogs} based on the first {@link PauseInfo} provided.
 *
 * <p>
 * The extractor identifies the first pause and returns all {@link TimeLog}
 * instances that appear <em>after</em> that pause index. This is typically used
 * to separate time logs occurring after a pause within a given day.
 * </p>
 *
 * <p>
 * This class is immutable and stateless.
 * </p>
 */
final class RightSegmentExtractor implements TimeLogsExtractor {

	/**
	 * Extracts the portion of the {@link TimeLog} list that comes after the first
	 * {@link PauseInfo}.
	 *
	 * <p>
	 * The method requires at least one pause and one time log to be present. The
	 * returned list contains all {@link TimeLog} elements whose index is greater
	 * than the index of the first pause.
	 * </p>
	 *
	 * @param date     the date associated with the time logs; can't be {@code null}
	 * @param timeLogs the complete list of time logs; can't be {@code null} or
	 *                 empty
	 * @param pauses   the list of pauses used to split the time logs; can't be
	 *                 {@code null} or empty
	 * @return a list containing the time logs located after the first pause; never
	 *         {@code null}
	 * @throws NullPointerException  if any argument is {@code null}
	 * @throws IllegalStateException if {@code pauses} or {@code timeLogs} is empty
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
		final TimeLog startLog = firstPause.after();

		final int fromIndex = timeLogs.indexOf(startLog);
		if (fromIndex < 0) {
			throw new IllegalStateException("Pause 'after' log not found in timeLogs");
		}
		return timeLogs.slice(fromIndex, timeLogs.size());
	}
}
