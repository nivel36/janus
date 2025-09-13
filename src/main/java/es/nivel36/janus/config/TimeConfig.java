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
package es.nivel36.janus.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring {@link Configuration} class that provides time-related beans.
 * <p>
 * Exposes a {@link Clock} bean representing the system's default time-zone.
 * <p>
 * Using {@link Clock} instead of calling {@code LocalDateTime.now()} directly
 * improves testability by allowing the clock to be injected and controlled in
 * unit tests (e.g., by providing a fixed or offset clock).
 *
 * <p>
 * Example usage in a service:
 * 
 * <pre>
 * &#64;Service
 * public class TimeLogService {
 * 	private final Clock clock;
 *
 * 	public TimeLogService(Clock clock) {
 * 		this.clock = clock;
 * 	}
 *
 * 	public Instant now() {
 * 		return clock(now);
 * 	}
 * }
 * </pre>
 */
@Configuration
public class TimeConfig {

	/**
	 * Provides a {@link Clock} instance based on the UTC time-zone.
	 * <p>
	 * This bean can be injected into services that require a source of current
	 * time, enabling deterministic testing.
	 *
	 * @return a system {@link Clock} using the UTC time-zone
	 */
	@Bean
	public Clock systemClock() {
		return Clock.systemUTC();
	}
}
