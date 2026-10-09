package es.nivel36.janus.service.timelog;

import java.time.Duration;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Unmodifiable collection of closed {@link TimeLog} references in entry-time
 * order.
 * <p>
 * Construction copies and sorts the supplied references. Each log must be
 * closed, and every exit must be strictly before the next entry: overlapping
 * and touching periods are rejected. An empty collection is valid.
 * <p>
 * The contained entities remain mutable. Later entity changes are visible and
 * are not revalidated, so the collection does not provide a thread-safety
 * guarantee.
 */
public final class TimeLogs implements Iterable<TimeLog> {

	private static final TimeLogs EMPTY = new TimeLogs(List.of());

	private final List<TimeLog> timeLogsList;

	/**
	 * Creates an ordered collection of closed logs with strictly separated periods.
	 *
	 * @param  timeLogs                 the logs to copy and sort; collection and
	 *                                  elements must not be {@code null}
	 * @throws NullPointerException     if the collection, an element or a required
	 *                                  entry time is {@code null}
	 * @throws IllegalArgumentException if a log is open or consecutive periods
	 *                                  overlap or touch
	 */
	public TimeLogs(final Collection<TimeLog> timeLogs) {
		Objects.requireNonNull(timeLogs, "timeLogsList cannot be null");

		if (timeLogs.stream().anyMatch(Objects::isNull)) {
			throw new NullPointerException("timeLogsList contains null elements");
		}

		final List<TimeLog> sorted = timeLogs.stream().filter(TimeLog::isClosed)
				.sorted(Comparator.comparing(TimeLog::getEntryTime)).toList();

		if (sorted.size() != timeLogs.size()) {
			throw new IllegalArgumentException("All TimeLogs must be closed");
		}
		assertNoOverlaps(sorted);
		this.timeLogsList = List.copyOf(sorted);
	}

	private static void assertNoOverlaps(final List<TimeLog> logs) {
		for (int i = 1; i < logs.size(); i++) {
			final TimeLog previous = logs.get(i - 1);
			final TimeLog current = logs.get(i);
			if (!previous.getExitTime().isBefore(current.getEntryTime())) {
				throw new IllegalArgumentException(
						String.format("Overlapping TimeLogs detected: %s and %s", previous, current));
			}
		}
	}

	/**
	 * Returns the unmodifiable list of log references in entry-time order.
	 *
	 * @return the stored list; contained entities remain mutable
	 */
	public List<TimeLog> asList() {
		return this.timeLogsList;
	}

	/**
	 * Returns the index of the specified {@link TimeLog} in this collection.
	 * <p>
	 * Equality is determined using {@link TimeLog#equals(Object)}.
	 * </p>
	 *
	 * @param  timeLog              the {@link TimeLog} to locate; must not be
	 *                              {@code null}
	 * @return                      the index of the time log, or {@code -1} if not
	 *                              present
	 * @throws NullPointerException if {@code timeLog} is {@code null}
	 */
	public int indexOf(final TimeLog timeLog) {
		Objects.requireNonNull(timeLog, "timeLog cannot be null");
		return this.timeLogsList.indexOf(timeLog);
	}

	/**
	 * Returns the sum of the recorded work durations of the contained logs.
	 *
	 * @return the total work duration, or {@link Duration#ZERO} for an empty
	 *         collection
	 */
	public Duration getTotalDuration() {
		return this.timeLogsList.stream().map(TimeLog::getWorkDuration).reduce(Duration.ZERO, Duration::plus);
	}

	/**
	 * Returns an iterator over the logs in ascending entry-time order.
	 *
	 * @return an iterator that does not support removal
	 */
	@Override
	public Iterator<TimeLog> iterator() {
		return this.timeLogsList.iterator();
	}

	public boolean isEmpty() {
		return this.timeLogsList.isEmpty();
	}

	public int size() {
		return this.timeLogsList.size();
	}

	/**
	 * Creates a new {@code TimeLogs} instance containing the elements in the
	 * specified range.
	 *
	 * @param  fromIndex                 the starting index (inclusive)
	 * @param  toIndex                   the ending index (exclusive)
	 * @return                           a new {@code TimeLogs} instance
	 * @throws IndexOutOfBoundsException if indices are out of range
	 * @throws IllegalArgumentException  if {@code fromIndex > toIndex}
	 */
	public TimeLogs slice(final int fromIndex, final int toIndex) {
		if (fromIndex < 0 || toIndex > this.timeLogsList.size()) {
			throw new IndexOutOfBoundsException(
					String.format("Indexs out of range: from=%d, to=%d", fromIndex, toIndex));
		}
		if (fromIndex > toIndex) {
			throw new IllegalArgumentException(
					String.format("fromIndex (%d) must be <= toIndex (%d)", fromIndex, toIndex));
		}
		return new TimeLogs(this.timeLogsList.subList(fromIndex, toIndex));
	}

	/**
	 * Returns an empty {@code TimeLogs} instance.
	 * <p>
	 * The returned instance represents a valid, immutable collection with no
	 * {@link TimeLog} elements.
	 * </p>
	 *
	 * @return an empty {@code TimeLogs}
	 */
	public static TimeLogs empty() {
		return EMPTY;
	}
}
