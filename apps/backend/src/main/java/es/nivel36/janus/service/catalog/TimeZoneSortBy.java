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
package es.nivel36.janus.service.catalog;

/**
 * Sorting strategies available for the time zone catalog endpoint.
 */
public enum TimeZoneSortBy {
	/**
	 * Sort by first zone id segment ({@code level1}), then by full zone id.
	 */
	LEVEL1,
	/**
	 * Sort by UTC offset string, then by full zone id.
	 */
	UTC
}
