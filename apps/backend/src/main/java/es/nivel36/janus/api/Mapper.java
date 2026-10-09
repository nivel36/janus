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
package es.nivel36.janus.api;

/**
 * Transforms source objects into values of a target type.
 * <p>
 * A {@code null} source produces {@code null}. Concrete mappers define the
 * transformation and any additional input requirements.
 *
 * @param <IN>  the source type
 * @param <OUT> the target type
 */
public interface Mapper<IN, OUT> {

	/**
	 * Maps the given input object to an instance of the output type. If the input
	 * is {@code null}, this method returns {@code null}.
	 *
	 * @param  object the source object to map; can be {@code null}
	 * @return        the mapped object of type {@code OUT}; {@code null} if the
	 *                {@code IN} object is {@code null}
	 */
	OUT map(IN object);
}
