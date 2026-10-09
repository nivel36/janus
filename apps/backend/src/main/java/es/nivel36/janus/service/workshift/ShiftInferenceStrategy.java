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
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package es.nivel36.janus.service.workshift;

import java.time.LocalDate;

import es.nivel36.janus.service.timelog.TimeLogs;

/**
 * Selects time logs belonging to a shift on a given local date.
 */
interface ShiftInferenceStrategy {

	/**
	 * Returns the time logs selected for the requested shift date.
	 *
	 * @param  date                 the local shift date; must not be {@code null}
	 * @param  orderedLogs          the candidate logs in chronological entry-time
	 *                              order; must not be {@code null}
	 * @return                      the selected logs, possibly empty
	 * @throws NullPointerException if either argument is {@code null}
	 */
	TimeLogs infer(LocalDate date, TimeLogs orderedLogs);
}
