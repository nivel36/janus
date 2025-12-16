package es.nivel36.janus.service.workshift;

import java.util.Optional;

import es.nivel36.janus.service.schedule.TimeRange;
import es.nivel36.janus.service.worksite.Worksite;

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
	 * @param worksite  the worksite associated with the shift. Can't be
	 *                  {@code null}.
	 * @param policy    the shift policy to apply. Can't be {@code null}.
	 * @return the resolved {@code ShiftInferenceStrategy} implementation matching
	 *         the provided context
	 * @throws NullPointerException if any of the parameters is {@code null}
	 */
	ShiftInferenceStrategy resolve(final Optional<TimeRange> timeRange, Worksite worksite, final ShiftPolicy policy) {
		return timeRange.<ShiftInferenceStrategy>map(tr -> new ScheduledShiftStrategy(policy, tr, worksite))
				.orElseGet(() -> new UnscheduledShiftStrategy(policy));
	}
}
