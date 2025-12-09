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
package es.nivel36.janus.api.v1.timelog;

import java.time.Duration;

import org.springframework.stereotype.Component;

import es.nivel36.janus.api.Mapper;

/**
 * {@link Mapper} implementation that converts a {@link Duration} into a
 * {@link DurationResponse} record.
 */
@Component
public class DurationResponseMapper implements Mapper<Duration, DurationResponse> {

	@Override
	public DurationResponse map(final Duration duration) {
		if (duration == null) {
			return null;
		}
		final long hours = duration.toHours();
		final int minutes = duration.toMinutesPart();
		final int secondsPart = duration.toSecondsPart();
		final String representation = duration.toString();
		return new DurationResponse(hours, minutes, secondsPart, representation);
	}
}
