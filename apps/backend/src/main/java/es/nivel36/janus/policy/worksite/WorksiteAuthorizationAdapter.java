package es.nivel36.janus.policy.worksite;

import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import es.nivel36.janus.security.Actor;
import es.nivel36.janus.security.ActorResolver;
import es.nivel36.janus.service.applicationsettings.ApplicationSettingsService;
import es.nivel36.janus.service.appuser.Role;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.employee.EmployeeService;
import es.nivel36.janus.service.worksite.WorksiteScope;

@Component("worksiteAuthorization")
public class WorksiteAuthorizationAdapter {
 private final ActorResolver actors; private final EmployeeService employees; private final ApplicationSettingsService settings;
 private final SearchWorksitePolicy search=new SearchWorksitePolicy(); private final ViewWorksitePolicy view=new ViewWorksitePolicy();
 private final ViewWorksiteStatsPolicy stats=new ViewWorksiteStatsPolicy(); private final CreateWorksitePolicy create=new CreateWorksitePolicy();
 private final UpdateWorksitePolicy update=new UpdateWorksitePolicy(); private final DeleteWorksitePolicy delete=new DeleteWorksitePolicy();
 private final ManageWorksiteAssignmentsPolicy assignments=new ManageWorksiteAssignmentsPolicy();
 public WorksiteAuthorizationAdapter(ActorResolver actors, EmployeeService employees, ApplicationSettingsService settings) { this.actors=Objects.requireNonNull(actors); this.employees=Objects.requireNonNull(employees); this.settings=Objects.requireNonNull(settings); }
 public boolean canSearch(Authentication auth,String email) { Actor a=actors.resolve(auth); return search.allows(a, owns(a,email)); }
 public String effectiveEmployeeEmail(Authentication auth,String requested) { Actor a=actors.resolve(auth); return restricted(a) ? employee(a).getEmail() : requested; }
 public boolean canView(Authentication auth) { return view.allows(actors.resolve(auth),null); }
 public boolean canViewStats(Authentication auth,String code) { Actor a=actors.resolve(auth); return stats.allows(a, assigned(a,code)); }
 public boolean canCreate(Authentication auth,WorksiteScope scope) { Actor a=actors.resolve(auth); return elevated(a) || create.allows(a,new CreateWorksitePolicy.Context(settings.isEmployeeWorkplaceCreationAllowed(),scope==WorksiteScope.ASSIGNED)); }
 public boolean canUpdate(Authentication auth,String code,WorksiteScope scope) { Actor a=actors.resolve(auth); return elevated(a) || update.allows(a,new UpdateWorksitePolicy.Context(settings.isEmployeeWorkplaceCreationAllowed(),scope==WorksiteScope.ASSIGNED,assigned(a,code))); }
 public boolean canDelete(Authentication auth) { return delete.allows(actors.resolve(auth),null); }
 public boolean canManageAssignments(Authentication auth) { return assignments.allows(actors.resolve(auth),null); }
 private boolean owns(Actor a,String email) { if(email==null||a.employeeId()==null)return false; try{return Objects.equals(a.employeeId(),employees.findEmployeeByEmail(email).getId());}catch(RuntimeException ex){return false;} }
 private boolean assigned(Actor a,String code) { return a.employeeId()!=null && employees.isAssignedToWorksite(employee(a).getEmail(),code); }
 private Employee employee(Actor a) { return employees.findEmployeeById(a.employeeId()); }
 private boolean elevated(Actor a) { return a.hasRole(Role.JANUS_USER)||a.hasRole(Role.JANUS_ADMIN); }
 private boolean restricted(Actor a) { return a.hasRole(Role.JANUS_EMPLOYEE)&&!a.hasRole(Role.JANUS_USER)&&!a.hasRole(Role.JANUS_ADMIN); }
}
