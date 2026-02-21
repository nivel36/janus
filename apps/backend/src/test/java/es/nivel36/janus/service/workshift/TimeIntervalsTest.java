package es.nivel36.janus.service.workshift;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class TimeIntervalsTest {

	@Test
	void testCreateTimeIntervalsFromEmptyListSuccess() {
		final TimeIntervals intervals = TimeIntervals.of(List.of());

		assertEquals(Duration.ZERO, intervals.totalCoveredDuration());
		assertEquals(Duration.ZERO, intervals.totalGapDuration());
	}

	@Test
	void testCreateTimeIntervalsWithSingleIntervalSuccess() {
		final TimeInterval interval = new TimeInterval( //
				Instant.parse("2025-01-01T10:00:00Z"), //
				Instant.parse("2025-01-01T11:00:00Z"));

		final TimeIntervals intervals = TimeIntervals.of(List.of(interval));

		assertEquals(Duration.ofHours(1), intervals.totalCoveredDuration());
		assertEquals(Duration.ZERO, intervals.totalGapDuration());
	}

	@Test
	void testCreateTimeIntervalsWithNullListFailure() {
		assertThrows(NullPointerException.class, () -> TimeIntervals.of(null));
	}

	@Test
	void testMergeOverlappingIntervalsSuccess() {
		final TimeInterval a = new TimeInterval( //
				Instant.parse("2025-01-01T10:00:00Z"), //
				Instant.parse("2025-01-01T11:00:00Z"));
		final TimeInterval b = new TimeInterval( //
				Instant.parse("2025-01-01T10:30:00Z"), //
				Instant.parse("2025-01-01T12:00:00Z"));

		final TimeIntervals intervals = TimeIntervals.of(List.of(a, b));

		assertEquals(Duration.ofHours(2), intervals.totalCoveredDuration());
		assertEquals(Duration.ZERO, intervals.totalGapDuration());
	}

	@Test
	void testMergeTouchingIntervalsSuccess() {
		final TimeInterval a = new TimeInterval( //
				Instant.parse("2025-01-01T10:00:00Z"), //
				Instant.parse("2025-01-01T11:00:00Z"));
		final TimeInterval b = new TimeInterval( //
				Instant.parse("2025-01-01T11:00:00Z"), //
				Instant.parse("2025-01-01T12:00:00Z"));

		final TimeIntervals intervals = TimeIntervals.of(List.of(a, b));

		assertEquals(Duration.ofHours(2), intervals.totalCoveredDuration());
		assertEquals(Duration.ZERO, intervals.totalGapDuration());
	}

	@Test
	void testMergeUnorderedIntervalsSuccess() {
		final TimeInterval a = new TimeInterval( //
				Instant.parse("2025-01-01T12:00:00Z"), //
				Instant.parse("2025-01-01T13:00:00Z"));
		final TimeInterval b = new TimeInterval( //
				Instant.parse("2025-01-01T10:00:00Z"), //
				Instant.parse("2025-01-01T11:00:00Z"));

		final TimeIntervals intervals = TimeIntervals.of(List.of(a, b));

		assertEquals(Duration.ofHours(2), intervals.totalCoveredDuration());
		assertEquals(Duration.ofHours(1), intervals.totalGapDuration());
	}

	@Test
	void testTotalCoveredDurationWithMultipleIntervalsSuccess() {
		final TimeInterval a = new TimeInterval( //
				Instant.parse("2025-01-01T08:00:00Z"), //
				Instant.parse("2025-01-01T09:00:00Z"));
		final TimeInterval b = new TimeInterval( //
				Instant.parse("2025-01-01T10:00:00Z"), //
				Instant.parse("2025-01-01T12:00:00Z"));

		final TimeIntervals intervals = TimeIntervals.of(List.of(a, b));

		assertEquals(Duration.ofHours(3), intervals.totalCoveredDuration());
	}

	@Test
	void testTotalGapDurationWithTwoIntervalsSuccess() {
		final TimeInterval a = new TimeInterval( //
				Instant.parse("2025-01-01T08:00:00Z"), //
				Instant.parse("2025-01-01T09:00:00Z"));
		final TimeInterval b = new TimeInterval( //
				Instant.parse("2025-01-01T10:30:00Z"), //
				Instant.parse("2025-01-01T11:30:00Z"));

		final TimeIntervals intervals = TimeIntervals.of(List.of(a, b));

		assertEquals(Duration.ofMinutes(90), intervals.totalGapDuration());
	}

	@Test
	void testTotalGapDurationWithMultipleIntervalsSuccess() {
		final TimeInterval a = new TimeInterval( //
				Instant.parse("2025-01-01T08:00:00Z"), //
				Instant.parse("2025-01-01T09:00:00Z"));
		final TimeInterval b = new TimeInterval( //
				Instant.parse("2025-01-01T10:00:00Z"), //
				Instant.parse("2025-01-01T11:00:00Z"));
		final TimeInterval c = new TimeInterval( //
				Instant.parse("2025-01-01T12:30:00Z"), //
				Instant.parse("2025-01-01T13:00:00Z"));

		final TimeIntervals intervals = TimeIntervals.of(List.of(a, b, c));

		assertEquals(Duration.ofMinutes(150), intervals.totalGapDuration());
	}

	@Test
	void testTotalGapDurationWithMergedIntervalsReturnsZero() {
		final TimeInterval a = new TimeInterval( //
				Instant.parse("2025-01-01T08:00:00Z"), //
				Instant.parse("2025-01-01T10:00:00Z"));
		final TimeInterval b = new TimeInterval( //
				Instant.parse("2025-01-01T09:30:00Z"), //
				Instant.parse("2025-01-01T11:00:00Z"));

		final TimeIntervals intervals = TimeIntervals.of(List.of(a, b));

		assertEquals(Duration.ZERO, intervals.totalGapDuration());
	}
}
