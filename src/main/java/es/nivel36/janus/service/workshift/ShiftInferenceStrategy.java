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
package es.nivel36.janus.service.workshift;

import java.time.LocalDate;

import es.nivel36.janus.service.timelog.TimeLogs;

/**
 * Strategy interface that decides which logs belong to a shift for a given
 * date/context.
 */
interface ShiftInferenceStrategy {

	/**
	 * Infers which logs belong to the shift described by the given context.
	 *
	 * @param context shift context, not null
	 * @return inference result with selected logs and an optional clip window
	 */
	TimeLogs infer(LocalDate date, TimeLogs orderedLogs);
}
