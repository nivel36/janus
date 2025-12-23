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

import es.nivel36.janus.service.timelog.TimeLog;
import es.nivel36.janus.service.timelog.TimeLogs;
import es.nivel36.janus.service.workshift.UnscheduledShiftStrategy.PauseInfo;

/**
 * Strategy abstraction used by {@link WorkShiftService} to collect the subset
 * of {@link TimeLog} entries that belong to a work shift depending on the
 * extraction scenario (weekday, week start, or week end).
 */
interface TimeLogsExtractor {

	/**
	 * Extracts the {@link TimeLog} entries that compose a work shift for the
	 * provided date, considering the ordered time logs and the long pauses detected
	 * beforehand.
	 *
	 * @param date     the target work shift date; must not be {@code null}
	 * @param timeLogs the chronologically ordered {@link TimeLog} list for the
	 *                 employee; must not be {@code null}
	 * @param pauses   the list of pauses identified for the employee on the date;
	 *                 must not be {@code null}
	 * @return the {@link TimeLog} entries that belong to the work shift
	 */
	TimeLogs extract(LocalDate date, TimeLogs timeLogs, List<PauseInfo> pauses);
}
