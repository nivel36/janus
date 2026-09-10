package es.nivel36.janus.api.v1;

import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/** Provisions legacy controller fixtures after their per-test SQL has run. */
public class EmployeeIdentityTestExecutionListener extends AbstractTestExecutionListener {
	@Override
	public int getOrder() { return Ordered.LOWEST_PRECEDENCE; }

	@Override
	public void beforeTestMethod(final TestContext testContext) {
		final JdbcTemplate jdbc = testContext.getApplicationContext().getBean(JdbcTemplate.class);
		jdbc.update("""
				INSERT INTO app_user(username,keycloak_subject,locale,time_format,default_timezone,employee_id)
				SELECT email,email,'en-US','H24','UTC',id FROM employee e
				WHERE NOT EXISTS (SELECT 1 FROM app_user u WHERE u.employee_id=e.id)
				""");
		jdbc.update("""
				INSERT INTO app_user(username,keycloak_subject,locale,time_format,default_timezone,employee_id)
				SELECT 'mock-actor','user','en-US','H24','UTC',NULL
				WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE keycloak_subject='user')
				""");
	}
}
