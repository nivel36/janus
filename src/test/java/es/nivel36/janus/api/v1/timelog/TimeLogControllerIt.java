package es.nivel36.janus.api.v1.timelog;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class TimeLogControllerIt {

	private @MockitoBean Clock clock;
	private @Autowired MockMvc mvc;
	private static final String BASE = "/api/v1/employee/{employeeEmail}/timelogs";

	@BeforeEach
	void beforeTest() {
		final Instant fixedNow = LocalDateTime.of(2025, 8, 8, 0, 0).toInstant(ZoneOffset.UTC);
		when(clock.instant()).thenReturn(fixedNow);
		when(clock.getZone()).thenReturn(ZoneOffset.UTC);
	}

	@Test
	@Sql(statements = { //
			"INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH','Standard Work Hours')",
			"INSERT INTO employee(name,surname,email, schedule_id) VALUES('Abel','Ferrer','aferrer@nivel36.es',1)",
			"INSERT INTO worksite(code,name,time_zone) VALUES('BCN-HQ','Barcelona Headquarters','UTC+2')"//
	})
	void testClockInShouldReturn201AndBody() throws Exception {
		String entry = "2025-08-04T09:30:00Z";

		mvc.perform(post(BASE + "/clock-in", "aferrer@nivel36.es") //
				.param("worksiteCode", "BCN-HQ") //
				.param("entryTime", entry)) //
				.andExpect(status().isCreated()) //
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON)) //
				.andExpect(jsonPath("$.entryTime").value(entry)) //
				.andExpect(jsonPath("$.worksiteCode").value("BCN-HQ"));
	}

	@Test
	@Sql(statements = { //
			"INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH','Standard Work Hours')",
			"INSERT INTO employee(name,surname,email, schedule_id) VALUES('Abel','Ferrer','aferrer@nivel36.es',1)",
			"INSERT INTO worksite(code,name,time_zone) VALUES('BCN-HQ','Barcelona Headquarters','UTC+2')"//
	})
	void testClockInWithDuplicatedEntryTimeShouldFail400() throws Exception {
		String entry = "2025-08-04T09:30:00Z";

		mvc.perform(post(BASE + "/clock-in", "aferrer@nivel36.es") //
				.param("worksiteCode", "BCN-HQ") //
				.param("entryTime", entry)) //
				.andExpect(status().isCreated());

		mvc.perform(post(BASE + "/clock-in", "aferrer@nivel36.es") //
				.param("worksiteCode", "BCN-HQ") //
				.param("entryTime", entry)) //
				.andExpect(status().isBadRequest()) //
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON));
	}

	@Test
	@Sql(statements = { //
			"INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH','Standard Work Hours')",
			"INSERT INTO employee(name,surname,email, schedule_id) VALUES('Abel','Ferrer','aferrer@nivel36.es',1)",
			"INSERT INTO worksite(code,name,time_zone) VALUES('BCN-HQ','Barcelona Headquarters','UTC+2')"//
	})
	void testClockInWithDuplicatedDeletedEntryTimeShouldReturn201AndBody() throws Exception {
		String entry = "2025-08-04T09:30:00Z";

		mvc.perform(post(BASE + "/clock-in", "aferrer@nivel36.es") //
				.param("worksiteCode", "BCN-HQ") //
				.param("entryTime", entry)) //
				.andExpect(status().isCreated());

		mvc.perform(delete(BASE + "/{entryTime}", "aferrer@nivel36.es", entry)) //
				.andExpect(status().isNoContent());

		mvc.perform(post(BASE + "/clock-in", "aferrer@nivel36.es") //
				.param("worksiteCode", "BCN-HQ") //
				.param("entryTime", entry)) //
				.andExpect(status().isCreated()) //
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON)) //
				.andExpect(jsonPath("$.entryTime").value(entry)) //
				.andExpect(jsonPath("$.worksiteCode").value("BCN-HQ"));
	}

	@Test
	@Sql(statements = { //
			"INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH','Standard Work Hours')",
			"INSERT INTO employee(name,surname,email, schedule_id) VALUES('Abel','Ferrer','aferrer@nivel36.es',1)",
			"INSERT INTO worksite(code,name,time_zone) VALUES('BCN-HQ','Barcelona Headquarters','UTC+2')"//
	})
	void testClockOutShouldReturn200AndBody() throws Exception {
		String entry = "2025-08-04T09:30:00Z";
		String exit = "2025-08-04T18:00:00Z";

		// seed: clock-in //
		mvc.perform(post(BASE + "/clock-in", "aferrer@nivel36.es") //
				.param("worksiteCode", "BCN-HQ") //
				.param("entryTime", entry)) //
				.andExpect(status().isCreated());

		// action: clock-out //
		mvc.perform(post(BASE + "/clock-out", "aferrer@nivel36.es") //
				.param("worksiteCode", "BCN-HQ") //
				.param("exitTime", exit)) //
				.andExpect(status().isOk()) //
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON)) //
				.andExpect(jsonPath("$.entryTime").value(entry)) //
				.andExpect(jsonPath("$.exitTime").value(exit)) //
				.andExpect(jsonPath("$.worksiteCode").value("BCN-HQ"));
	}

	@Test
	@Sql(statements = { //
			"INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH','Standard Work Hours')",
			"INSERT INTO employee(name,surname,email, schedule_id) VALUES('Abel','Ferrer','aferrer@nivel36.es',1)",
			"INSERT INTO worksite(code,name,time_zone) VALUES('BCN-HQ','Barcelona Headquarters','UTC+2')"//
	})
	void testCreateTimeLogShouldReturn200AndBody() throws Exception {
		String entry = "2025-08-05T09:00:00Z";
		String exit = "2025-08-05T17:30:00Z";
		String body = """
				  {"entryTime":"%s","exitTime":"%s"}
				""".formatted(entry, exit);

		mvc.perform(post(BASE + "/timelogs", "aferrer@nivel36.es") //
				.param("worksiteCode", "BCN-HQ") //
				.contentType(APPLICATION_JSON).content(body)) //
				.andExpect(status().isOk()) //
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON)) //
				.andExpect(jsonPath("$.entryTime").value(entry)) //
				.andExpect(jsonPath("$.exitTime").value(exit)) //
				.andExpect(jsonPath("$.worksiteCode").value("BCN-HQ"));
	}

	@Test
	@Sql(statements = { //
			"INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH','Standard Work Hours')",
			"INSERT INTO employee(name,surname,email, schedule_id) VALUES('Abel','Ferrer','aferrer@nivel36.es',1)",
			"INSERT INTO worksite(code,name,time_zone) VALUES('BCN-HQ','Barcelona Headquarters','UTC+2')"//
	})
	void testCreateDuplicatedTimeLogShouldReturn400() throws Exception {
		String entry = "2025-08-05T09:00:00Z";
		String exit = "2025-08-05T17:30:00Z";
		String body = """
				  {"entryTime":"%s","exitTime":"%s"}
				""".formatted(entry, exit);

		mvc.perform(post(BASE + "/timelogs", "aferrer@nivel36.es") //
				.param("worksiteCode", "BCN-HQ") //
				.contentType(APPLICATION_JSON).content(body)) //
				.andExpect(status().isOk());

		mvc.perform(post(BASE + "/timelogs", "aferrer@nivel36.es") //
				.param("worksiteCode", "BCN-HQ") //
				.contentType(APPLICATION_JSON).content(body)) //
				.andExpect(status().isBadRequest());
	}

	@Test
	@Sql(statements = { //
			"INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH','Standard Work Hours')",
			"INSERT INTO employee(name,surname,email, schedule_id) VALUES('Abel','Ferrer','aferrer@nivel36.es',1)",
			"INSERT INTO worksite(code,name,time_zone) VALUES('BCN-HQ','Barcelona Headquarters','UTC+2')"//
	})
	void testSearchByEmployeeShouldReturn200() throws Exception {
		// seed: one log //
		mvc.perform(post(BASE + "/clock-in", "aferrer@nivel36.es") //
				.param("worksiteCode", "BCN-HQ") //
				.param("entryTime", "2025-08-06T08:00:00Z")) //
				.andExpect(status().isCreated());

		mvc.perform(get(BASE + "/", "aferrer@nivel36.es")) //
				.andExpect(status().isOk()) //
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON));
	}

	@Test
	@Sql(statements = { //
			"INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH','Standard Work Hours')",
			"INSERT INTO employee(name,surname,email, schedule_id) VALUES('Abel','Ferrer','aferrer@nivel36.es',1)",
			"INSERT INTO worksite(code,name,time_zone) VALUES('BCN-HQ','Barcelona Headquarters','UTC+2')"//
	})
	void testSearchByEmployeeWithInvalidRangeShouldFail400() throws Exception {
		mvc.perform(get(BASE + "/", "aferrer@nivel36.es") //
				.param("fromInstant", "2025-08-10T10:00:00Z")) //
				.andExpect(status().isBadRequest());

		mvc.perform(get(BASE + "/", "aferrer@nivel36.es") //
				.param("fromInstant", "2025-08-10T10:00:00Z") //
				.param("toInstant", "2025-08-09T10:00:00Z")) //
				.andExpect(status().isBadRequest());
	}

	@Test
	@Sql(statements = { //
			"INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH','Standard Work Hours')",
			"INSERT INTO employee(name,surname,email, schedule_id) VALUES('Abel','Ferrer','aferrer@nivel36.es',1)",
			"INSERT INTO worksite(code,name,time_zone) VALUES('BCN-HQ','Barcelona Headquarters','UTC+2')"//
	})
	void testFindTimeLogByEmployeeAndEntryTimeShouldReturnBody() throws Exception {
		String entry = "2025-08-07T07:45:00Z";

		// seed //
		mvc.perform(post(BASE + "/clock-in", "aferrer@nivel36.es") //
				.param("worksiteCode", "BCN-HQ") //
				.param("entryTime", entry)) //
				.andExpect(status().isCreated());

		mvc.perform(get(BASE + "/{entryTime}", "aferrer@nivel36.es", entry)) //
				.andExpect(status().isOk()) //
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON)) //
				.andExpect(jsonPath("$.entryTime").value(entry)) //
				.andExpect(jsonPath("$.worksiteCode").value("BCN-HQ"));
	}

	@Test
	@Sql(statements = { //
			"INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH','Standard Work Hours')",
			"INSERT INTO employee(name,surname,email, schedule_id) VALUES('Abel','Ferrer','aferrer@nivel36.es',1)",
			"INSERT INTO worksite(code,name,time_zone) VALUES('BCN-HQ','Barcelona Headquarters','UTC+2')"//
	})
	void testGetHoursWorkedShouldReturnDuration() throws Exception {
		String entry = "2025-08-08T09:00:00Z";
		String exit = "2025-08-08T17:30:00Z";

		// seed //
		mvc.perform(post(BASE + "/timelogs", "aferrer@nivel36.es") //
				.param("worksiteCode", "BCN-HQ") //
				.contentType(APPLICATION_JSON).content("""
						{"entryTime":"%s","exitTime":"%s"}
						""".formatted(entry, exit))) //
				.andExpect(status().isOk());

		mvc.perform(get(BASE + "/{entryTime}/time-worked", "aferrer@nivel36.es", entry)) //
				.andExpect(status().isOk()) //
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON)) //
				.andExpect(jsonPath("$.iso8601").value("PT8H30M")) //
				.andExpect(jsonPath("$.hours").value(8)) //
				.andExpect(jsonPath("$.minutes").value(30));
	}

	@Test
	@Sql(statements = { //
			"INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH','Standard Work Hours')",
			"INSERT INTO employee(name,surname,email, schedule_id) VALUES('Abel','Ferrer','aferrer@nivel36.es',1)",
			"INSERT INTO worksite(code,name,time_zone) VALUES('BCN-HQ','Barcelona Headquarters','UTC+2')"//
	})
	void testDeleteShouldReturn204AndFindReturns404() throws Exception {
		String entry = "2025-08-09T06:30:00Z";

		// seed //
		mvc.perform(post(BASE + "/clock-in", "aferrer@nivel36.es") //
				.param("worksiteCode", "BCN-HQ") //
				.param("entryTime", entry)) //
				.andExpect(status().isCreated());

		// delete //
		mvc.perform(delete(BASE + "/{entryTime}", "aferrer@nivel36.es", entry)) //
				.andExpect(status().isNoContent());

		// verify //
		mvc.perform(get(BASE + "/{entryTime}", "aferrer@nivel36.es", entry)) //
				.andExpect(status().isNotFound());
	}

	@Test
	void testClockInWithInvalidEmailShouldFail400() throws Exception {
		mvc.perform(post(BASE + "/clock-in", "bad email") //
				.param("worksiteCode", "BCN-HQ")) //
				.andExpect(status().isBadRequest());
	}

	@Test
	void testClockInWithInvalidWorksiteCodeShouldFail400() throws Exception {
		mvc.perform(post(BASE + "/clock-in", "aferrer@nivel36.es") //
				.param("worksiteCode", "BAD CODE")) //
				.andExpect(status().isBadRequest());
	}
}