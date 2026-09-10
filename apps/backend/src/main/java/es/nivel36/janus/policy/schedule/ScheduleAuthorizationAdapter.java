package es.nivel36.janus.policy.schedule;
import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import es.nivel36.janus.security.Actor; import es.nivel36.janus.security.ActorResolver; import es.nivel36.janus.service.appuser.Role; import es.nivel36.janus.service.employee.EmployeeService;
@Component("scheduleAuthorization") public class ScheduleAuthorizationAdapter {
 private final ActorResolver actors; private final EmployeeService employees; private final SearchSchedulePolicy search=new SearchSchedulePolicy(); private final ViewSchedulePolicy view=new ViewSchedulePolicy(); private final CreateSchedulePolicy create=new CreateSchedulePolicy(); private final UpdateSchedulePolicy update=new UpdateSchedulePolicy(); private final DeleteSchedulePolicy delete=new DeleteSchedulePolicy();
 public ScheduleAuthorizationAdapter(ActorResolver actors,EmployeeService employees){this.actors=Objects.requireNonNull(actors);this.employees=Objects.requireNonNull(employees);}
 public boolean canSearch(Authentication auth,String email){Actor a=actors.resolve(auth);return search.allows(a,owns(a,email));}
 public String effectiveEmployeeEmail(Authentication auth,String requested){Actor a=actors.resolve(auth);return restricted(a)?employees.findEmployeeById(a.employeeId()).getEmail():requested;}
 public boolean canView(Authentication auth,String code){Actor a=actors.resolve(auth);return view.allows(a,a.employeeId()!=null&&employees.isAssignedToSchedule(employees.findEmployeeById(a.employeeId()).getEmail(),code));}
 public boolean canCreate(Authentication a){return create.allows(actors.resolve(a),null);} public boolean canUpdate(Authentication a){return update.allows(actors.resolve(a),null);} public boolean canDelete(Authentication a){return delete.allows(actors.resolve(a),null);}
 private boolean owns(Actor a,String email){if(email==null||a.employeeId()==null)return false;try{return Objects.equals(a.employeeId(),employees.findEmployeeByEmail(email).getId());}catch(RuntimeException e){return false;}}
 private boolean restricted(Actor a){return a.hasRole(Role.JANUS_EMPLOYEE)&&!a.hasRole(Role.JANUS_USER)&&!a.hasRole(Role.JANUS_ADMIN);}
}
