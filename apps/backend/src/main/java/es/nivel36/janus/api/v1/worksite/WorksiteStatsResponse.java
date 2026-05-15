package es.nivel36.janus.api.v1.worksite;

import java.time.Instant;

public record WorksiteStatsResponse(String worksiteCode, Instant startInclusive, Instant endInclusive,
		long employeesWhoClockedIn, long erroneousTimeLogs, long totalTimeLogs, long employeesAllowedToClockIn,
		long distinctSchedulesFromEmployeesWhoClockedIn) {
}
