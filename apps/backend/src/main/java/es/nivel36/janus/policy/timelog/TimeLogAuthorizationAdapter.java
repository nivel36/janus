package es.nivel36.janus.policy.timelog;
import java.util.Objects; import org.springframework.security.core.Authentication; import org.springframework.stereotype.Component;
import es.nivel36.janus.security.Actor; import es.nivel36.janus.security.ActorResolver; import es.nivel36.janus.service.applicationsettings.ApplicationSettingsService; import es.nivel36.janus.service.appuser.Role; import es.nivel36.janus.service.employee.EmployeeService;
@Component("timeLogAuthorization") public class TimeLogAuthorizationAdapter {
 private final ActorResolver actors; private final EmployeeService employees; private final ApplicationSettingsService settings; private final OperateTimeLogPolicy operate=new OperateTimeLogPolicy(); private final ViewTimeLogPolicy view=new ViewTimeLogPolicy(); private final DeleteTimeLogPolicy delete=new DeleteTimeLogPolicy();
 public TimeLogAuthorizationAdapter(ActorResolver a,EmployeeService e,ApplicationSettingsService s){actors=Objects.requireNonNull(a);employees=Objects.requireNonNull(e);settings=Objects.requireNonNull(s);}
 public boolean canOperate(Authentication auth,String email,boolean manual){Actor a=actors.resolve(auth);return operate.allows(a,new OperateTimeLogPolicy.Context(owns(a,email),manual,settings.isEmployeeManualTimelogEntryAllowed()));}
 public boolean canView(Authentication auth,String email){Actor a=actors.resolve(auth);return view.allows(a,!restricted(a)||owns(a,email));}
 public boolean canDelete(Authentication auth){return delete.allows(actors.resolve(auth),null);}
 private boolean owns(Actor a,String email){if(a.employeeId()==null)return false;try{return Objects.equals(a.employeeId(),employees.findEmployeeByEmail(email).getId());}catch(RuntimeException e){return false;}}
 private boolean restricted(Actor a){return a.hasRole(Role.JANUS_EMPLOYEE)&&!a.hasRole(Role.JANUS_USER)&&!a.hasRole(Role.JANUS_ADMIN);}
}
