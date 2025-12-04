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
package es.nivel36.janus.api.v1.appuser;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class AppUserControllerIT {

        private @Autowired MockMvc mvc;

        private static final String BASE = "/api/v1/appusers";

        @Test
        @Sql(statements = { //
                        "INSERT INTO app_user(username,name,surname,locale,time_format) VALUES('jdoe','John','Doe','en-US','H24')"//
        })
        void testFindByUsernameShouldReturnUser() throws Exception {
                mvc.perform(get(BASE + "/{username}", "jdoe")).andExpect(status().isOk()) //
                                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON)) //
                                .andExpect(jsonPath("$.username").value("jdoe")) //
                                .andExpect(jsonPath("$.name").value("John")) //
                                .andExpect(jsonPath("$.surname").value("Doe")) //
                                .andExpect(jsonPath("$.locale").value("en-US")) //
                                .andExpect(jsonPath("$.timeFormat").value("H24"));
        }

        @Test
        void testFindUnknownUserShouldReturn404() throws Exception {
                mvc.perform(get(BASE + "/{username}", "unknown")) //
                                .andExpect(status().isNotFound());
        }

        @Test
        void testFindWithInvalidPatternShouldFail400() throws Exception {
                mvc.perform(get(BASE + "/{username}", "bad user")) //
                                .andExpect(status().isBadRequest());
        }

        @Test
        @Sql(statements = { //
                        "INSERT INTO app_user(username,name,surname,locale,time_format) VALUES('jdoe','John','Doe','en-US','H24')"//
        })
        void testCreateAlreadyExistsShouldReturn400() throws Exception {
                String body = """
                                  {"username":"jdoe","name":"John","surname":"Doe","locale":"en-US","timeFormat":"H24"}
                                """;

                mvc.perform(post(BASE).contentType(APPLICATION_JSON).content(body)) //
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testCreateShouldReturn201AndBody() throws Exception {
                String body = """
                                  {"username":"asmith","name":"Alice","surname":"Smith","locale":"en-GB","timeFormat":"H12"}
                                """;

                mvc.perform(post(BASE).contentType(APPLICATION_JSON).content(body)) //
                                .andExpect(status().isCreated()) //
                                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON)) //
                                .andExpect(jsonPath("$.username").value("asmith")) //
                                .andExpect(jsonPath("$.name").value("Alice")) //
                                .andExpect(jsonPath("$.surname").value("Smith")) //
                                .andExpect(jsonPath("$.locale").value("en-GB")) //
                                .andExpect(jsonPath("$.timeFormat").value("H12"));
                mvc.perform(get(BASE + "/{username}", "asmith")).andExpect(status().isOk());
        }

        @Test
        @Sql(statements = { //
                        "INSERT INTO app_user(username,name,surname,locale,time_format) VALUES('jdoe','John','Doe','en-US','H24')"//
        })
        void testUpdateShouldReturn200AndUpdatedBody() throws Exception {
                String body = """
                                  {"name":"Jonathan","surname":"Doe","locale":"en-CA","timeFormat":"H12"}
                                """;

                mvc.perform(put(BASE + "/{username}", "jdoe").contentType(APPLICATION_JSON).content(body)) //
                                .andExpect(status().isOk()) //
                                .andExpect(jsonPath("$.username").value("jdoe")) //
                                .andExpect(jsonPath("$.name").value("Jonathan")) //
                                .andExpect(jsonPath("$.surname").value("Doe")) //
                                .andExpect(jsonPath("$.locale").value("en-CA")) //
                                .andExpect(jsonPath("$.timeFormat").value("H12"));
        }

        @Test
        @Sql(statements = { //
                        "INSERT INTO app_user(username,name,surname,locale,time_format) VALUES('jdoe','John','Doe','en-US','H24')"//
        })
        void testDeleteShouldReturn204AndRemoveFromList() throws Exception {
                mvc.perform(delete(BASE + "/{username}", "jdoe")) //
                                .andExpect(status().isNoContent());

                mvc.perform(get(BASE + "/{username}", "jdoe")) //
                                .andExpect(status().isNotFound()); //
        }
}
