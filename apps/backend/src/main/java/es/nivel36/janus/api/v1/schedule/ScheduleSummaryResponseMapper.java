/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package es.nivel36.janus.api.v1.schedule;

import org.springframework.stereotype.Component;

import es.nivel36.janus.api.Mapper;
import es.nivel36.janus.service.schedule.Schedule;

/**
 * Maps {@link Schedule} entities to summaries without accessing their rules.
 */
@Component
public class ScheduleSummaryResponseMapper implements Mapper<Schedule, ScheduleSummaryResponse> {

	@Override
	public ScheduleSummaryResponse map(final Schedule schedule) {
		if (schedule == null) {
			return null;
		}
		return new ScheduleSummaryResponse(
				schedule.getCode(),
				schedule.getName(),
				schedule.getEntryTolerance(),
				schedule.getExitTolerance());
	}
}
