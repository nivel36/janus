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
package es.nivel36.janus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@SpringBootApplication
@EntityScan("es.nivel36.janus.service")
@EnableJpaRepositories(basePackages = "es.nivel36.janus.service")
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class JanusApplication {

	/**
	 * Boots the Spring application context for Janus.
	 * <p>
	 * Delegates to {@link SpringApplication#run(Class, String...)} to initialize
	 * all configured beans and start the embedded web server.
	 *
	 * @param args optional command-line arguments forwarded to Spring Boot
	 */
	public static void main(String[] args) {
		SpringApplication.run(JanusApplication.class, args);
	}
}
