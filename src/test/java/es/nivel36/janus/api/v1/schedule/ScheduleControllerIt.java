package es.nivel36.janus.api.v1.schedule;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
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
class ScheduleControllerIt {

	private static final String BASE = "/api/v1/schedules";

	private @Autowired MockMvc mvc;

	@Test
	void testCreateScheduleShouldReturn201AndBody() throws Exception {
		String body = """
				{
				  "code": "STD-WH",
				  "name": "Standard Work Hours",
				  "rules": [
				    {
				      "name": "Weekday Rule",
				      "dayOfWeekRanges": [
				        {
				          "dayOfWeek": "MONDAY",
				          "effectiveWorkHours": "PT8H",
				          "timeRange": {
				            "startTime": "09:00",
				            "endTime": "17:00"
				          }
				        }
				      ]
				    }
				  ]
				}
				""";

		mvc.perform(post(BASE).contentType(APPLICATION_JSON).content(body)) //
				.andExpect(status().isCreated()) //
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON)) //
				.andExpect(jsonPath("$.code").value("STD-WH")) //
				.andExpect(jsonPath("$.rules[0].dayOfWeekRanges[0].effectiveWorkHours").value("PT8H")) //
				.andExpect(jsonPath("$.rules[0].dayOfWeekRanges[0].timeRange.startTime").value("09:00:00"));
	}

	@Test
	void testCreateDuplicatedScheduleShouldReturn400() throws Exception {
		String body = """
				{
				  "code": "STD-WH",
				  "name": "Standard Work Hours",
				  "rules": [
				    {
				      "name": "Weekday Rule",
				      "dayOfWeekRanges": [
				        {
				          "dayOfWeek": "MONDAY",
				          "effectiveWorkHours": "PT8H",
				          "timeRange": {
				            "startTime": "09:00",
				            "endTime": "17:00"
				          }
				        }
				      ]
				    }
				  ]
				}
				""";

		mvc.perform(post(BASE).contentType(APPLICATION_JSON).content(body)) //
				.andExpect(status().isCreated());

		mvc.perform(post(BASE).contentType(APPLICATION_JSON).content(body)) //
				.andExpect(status().isBadRequest()) //
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON));
	}

	@Test
	void testGetSchedulesShouldReturn200AndBody() throws Exception {
		String body = """
				{
				  "code": "STD-WH",
				  "name": "Standard Work Hours",
				  "rules": [
				    {
				      "name": "Weekday Rule",
				      "dayOfWeekRanges": [
				        {
				          "dayOfWeek": "MONDAY",
				          "effectiveWorkHours": "PT8H",
				          "timeRange": {
				            "startTime": "09:00",
				            "endTime": "17:00"
				          }
				        }
				      ]
				    }
				  ]
				}
				""";

		mvc.perform(post(BASE).contentType(APPLICATION_JSON).content(body)) //
				.andExpect(status().isCreated());

		mvc.perform(get(BASE)) //
				.andExpect(status().isOk()) //
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON)) //
				.andExpect(jsonPath("$[0].code").value("STD-WH"));
	}

	@Test
	void testFindScheduleShouldReturn200AndBody() throws Exception {
		String body = """
				{
				  "code": "STD-WH",
				  "name": "Standard Work Hours",
				  "rules": [
				    {
				      "name": "Weekday Rule",
				      "dayOfWeekRanges": [
				        {
				          "dayOfWeek": "MONDAY",
				          "effectiveWorkHours": "PT8H",
				          "timeRange": {
				            "startTime": "09:00",
				            "endTime": "17:00"
				          }
				        }
				      ]
				    }
				  ]
				}
				""";

		mvc.perform(post(BASE).contentType(APPLICATION_JSON).content(body)) //
				.andExpect(status().isCreated());

		mvc.perform(get(BASE + "/{code}", "STD-WH")) //
				.andExpect(status().isOk()) //
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON)) //
				.andExpect(jsonPath("$.name").value("Standard Work Hours")) //
				.andExpect(jsonPath("$.rules[0].dayOfWeekRanges[0].dayOfWeek").value("MONDAY"));
	}

	@Test
	void testUpdateScheduleShouldReturn200AndBody() throws Exception {
		String createBody = """
				{
				  "code": "STD-WH",
				  "name": "Standard Work Hours",
				  "rules": [
				    {
				      "name": "Weekday Rule",
				      "dayOfWeekRanges": [
				        {
				          "dayOfWeek": "MONDAY",
				          "effectiveWorkHours": "PT8H",
				          "timeRange": {
				            "startTime": "09:00",
				            "endTime": "17:00"
				          }
				        }
				      ]
				    }
				  ]
				}
				""";

		mvc.perform(post(BASE).contentType(APPLICATION_JSON).content(createBody)) //
				.andExpect(status().isCreated());

		String updateBody = """
				{
				  "name": "Updated Work Hours",
				  "rules": [
				    {
				      "name": "Weekend Rule",
				      "dayOfWeekRanges": [
				        {
				          "dayOfWeek": "SATURDAY",
				          "effectiveWorkHours": "PT6H",
				          "timeRange": {
				            "startTime": "10:00",
				            "endTime": "16:00"
				          }
				        }
				      ]
				    }
				  ]
				}
				""";

		mvc.perform(put(BASE + "/{code}", "STD-WH").contentType(APPLICATION_JSON).content(updateBody)) //
				.andExpect(status().isOk()) //
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON)) //
				.andExpect(jsonPath("$.name").value("Updated Work Hours")) //
				.andExpect(jsonPath("$.rules[0].dayOfWeekRanges[0].dayOfWeek").value("SATURDAY"));
	}

	@Test
	void testDeleteScheduleShouldReturn204() throws Exception {
		String body = """
				{
				  "code": "STD-WH",
				  "name": "Standard Work Hours",
				  "rules": [
				    {
				      "name": "Weekday Rule",
				      "dayOfWeekRanges": [
				        {
				          "dayOfWeek": "MONDAY",
				          "effectiveWorkHours": "PT8H",
				          "timeRange": {
				            "startTime": "09:00",
				            "endTime": "17:00"
				          }
				        }
				      ]
				    }
				  ]
				}
				""";

		mvc.perform(post(BASE).contentType(APPLICATION_JSON).content(body)) //
				.andExpect(status().isCreated());

		mvc.perform(delete(BASE + "/{code}", "STD-WH")) //
				.andExpect(status().isNoContent());
	}

	@Test
	@Sql(statements = { "INSERT INTO schedule(id,code,name) VALUES (1,'IN-USE','In Use Schedule')",
			"INSERT INTO employee(id,name,surname,email,schedule_id) VALUES (1,'Abel','Ferrer','aferrer@nivel36.es',1)" })
	void testDeleteScheduleWithAssignedEmployeesShouldReturn409() throws Exception {
		mvc.perform(delete(BASE + "/{code}", "IN-USE")) //
				.andExpect(status().isConflict()) //
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON));
	}
}
