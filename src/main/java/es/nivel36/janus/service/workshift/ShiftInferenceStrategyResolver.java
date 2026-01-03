/*
 * Copyright 2026 Abel Ferrer Jiménez
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

import java.time.ZoneId;
import java.util.Optional;

import es.nivel36.janus.service.schedule.TimeRange;

/**
 * Resolves the appropriate {@link ShiftInferenceStrategy} based on the presence
 * of a scheduled time range and the applicable shift policy.
 *
 * <p>
 * If a {@link TimeRange} is provided, a {@link ScheduledShiftStrategy} is
 * created using the given policy and worksite. Otherwise, an
 * {@link UnscheduledShiftStrategy} is used.
 */
class ShiftInferenceStrategyResolver {

	/**
	 * Resolves and returns a {@link ShiftInferenceStrategy} according to the
	 * specified parameters.
	 *
	 * @param timeRange an optional scheduled time range for the shift. If present,
	 *                  it influences the strategy selection. Can't be {@code null}.
	 * @param timeZone  the timeZone associated with the shift. Can't be
	 *                  {@code null}.
	 * @param policy    the shift policy to apply. Can't be {@code null}.
	 * @return the resolved {@code ShiftInferenceStrategy} implementation matching
	 *         the provided context
	 * @throws NullPointerException if any of the parameters is {@code null}
	 */
	ShiftInferenceStrategy resolve(final Optional<TimeRange> timeRange, ZoneId timeZone, final ShiftPolicy policy) {
		return timeRange.<ShiftInferenceStrategy>map(tr -> new ScheduledShiftStrategy(policy, tr, timeZone))
				.orElseGet(() -> new UnscheduledShiftStrategy(policy, timeZone));
	}
}
