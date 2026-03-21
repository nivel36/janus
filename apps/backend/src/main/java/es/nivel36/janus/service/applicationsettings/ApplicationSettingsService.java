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
package es.nivel36.janus.service.applicationsettings;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.nivel36.janus.service.ResourceNotFoundException;
import es.nivel36.janus.service.timelog.TimeLog;

@Service
public class ApplicationSettingsService {

	static final int DEFAULT_DAYS_UNTIL_LOCKED = 7;

	private static final Logger logger = LoggerFactory.getLogger(ApplicationSettingsService.class);

	private final ApplicationSettingsRepository applicationSettingsRepository;

	public ApplicationSettingsService(final ApplicationSettingsRepository applicationSettingsRepository) {
		this.applicationSettingsRepository = Objects.requireNonNull(applicationSettingsRepository,
				"applicationSettingsRepository cannot be null");
	}

	/**
	 * Returns the number of days during which a {@link TimeLog} remains modifiable
	 * before it becomes locked.
	 *
	 * @return the number of days left until the {@link TimeLog} can no longer be
	 *         modified
	 */
	@Transactional(readOnly = true)
	public int getDaysUntilLocked() {
		logger.debug("Loading admin configuration to resolve daysUntilLocked");
		return this.applicationSettingsRepository.findFirstByOrderByIdAsc().map(ApplicationSettings::getDaysUntilLocked)
				.orElseGet(() -> {
					logger.warn("Application settings are missing; falling back to default daysUntilLocked={}",
							DEFAULT_DAYS_UNTIL_LOCKED);
					return DEFAULT_DAYS_UNTIL_LOCKED;
				});
	}

	@Transactional(readOnly = true)
	ApplicationSettings getApplicationSettings() {
		return this.findApplicationSettings();
	}

	private ApplicationSettings findApplicationSettings() {
		return this.applicationSettingsRepository.findFirstByOrderByIdAsc().orElseThrow(
				() -> new ResourceNotFoundException("Application settings have not been configured yet"));
	}
}
