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

final class WeekdayTimeLogsExtractor implements TimeLogsExtractor {

	@Override
	public List<TimeLog> extract(LocalDate date, List<TimeLog> timeLogs, List<PauseInfo> pauses) {
		Objects.requireNonNull(date, "Date must not be null");
		Objects.requireNonNull(timeLogs, "TimeLogs must not be null");
		Objects.requireNonNull(pauses, "Pauses must not be null");

		if (pauses.size() < 2) {
			throw new IllegalArgumentException("At least two long pauses are required to extract weekday logs");
		}
		if (timeLogs.size() < 2) {
			throw new IllegalArgumentException("At least two timelogs are needed");
		}

		// Ordena por duración descendente y, a igualdad, por índice ascendente
		pauses.sort((a, b) -> {
			final int byDuration = b.duration.compareTo(a.duration);
			return (byDuration != 0) ? byDuration : Integer.compare(a.index, b.index);
		});

		// Toma las dos más largas
		final PauseInfo p1 = pauses.get(0);
		final PauseInfo p2 = pauses.get(1);

		// Verifica índices válidos respecto a timeLogs
		final int maxIdx = timeLogs.size() - 1;
		if (p1.index < 0 || p1.index > maxIdx - 1 || p2.index < 0 || p2.index > maxIdx - 1) {
			throw new IllegalStateException("Pause indexs out of bounds for timeLogs");
		}

		// Asegura orden
		final int leftPauseIndex = Math.min(p1.index, p2.index);
		final int rightPauseIndex = Math.max(p1.index, p2.index);

		// Segmento entre ambas pausas [left+1, right] inclusivo
		final int startIndex = Math.max(0, leftPauseIndex + 1);
		final int endIndex = Math.min(maxIdx, rightPauseIndex);

		if (startIndex > endIndex) {
			// No hay tramo válido entre pausas
			return List.of();
		}

		// Copia defensiva para evitar vista de subList
		return new ArrayList<>(timeLogs.subList(startIndex, endIndex + 1));
	}
}
