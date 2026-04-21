package es.nivel36.janus.util;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;

import es.nivel36.janus.service.appuser.Role;

/**
 * Utility class for working with Spring Security authorities.
 * <p>
 * This class centralizes role name definitions and provides helper methods
 * to check whether a given collection of {@link GrantedAuthority} contains
 * specific roles or only a specific role.
 * </p>
 * <p>
 * Role names follow the Spring Security convention: {@code ROLE_<NAME>}.
 * </p>
 */
public final class Roles {

	public static final String ADMIN = authority(Role.JANUS_ADMIN);
	public static final String USER = authority(Role.JANUS_USER);
	public static final String EMPLOYEE = authority(Role.JANUS_EMPLOYEE);

	private Roles() {
	}

	/**
	 * Determines whether the given authorities contain the administrator role.
	 *
	 * @param authorities the authorities to inspect
	 * @return {@code true} if the administrator role is present; {@code false} otherwise
	 */
	public static boolean hasAdminRole(Collection<? extends GrantedAuthority> authorities) {
		return hasRole(authorities, ADMIN);
	}

	/**
	 * Determines whether the given authorities contain the user role.
	 *
	 * @param authorities the authorities to inspect
	 * @return {@code true} if the user role is present; {@code false} otherwise
	 */
	public static boolean hasUserRole(Collection<? extends GrantedAuthority> authorities) {
		return hasRole(authorities, USER);
	}

	/**
	 * Determines whether the given authorities contain the employee role.
	 *
	 * @param authorities the authorities to inspect
	 * @return {@code true} if the employee role is present; {@code false} otherwise
	 */
	public static boolean hasEmployeeRole(Collection<? extends GrantedAuthority> authorities) {
		return hasRole(authorities, EMPLOYEE);
	}

	/**
	 * Determines whether the given authorities contain exclusively the employee role.
	 * <p>
	 * This method returns {@code true} only if the set of authorities contains exactly
	 * one role and that role is {@code EMPLOYEE}.
	 * </p>
	 *
	 * @param authorities the authorities to inspect
	 * @return {@code true} if the only role present is employee; {@code false} otherwise
	 */
	public static boolean hasOnlyEmployeeRole(Collection<? extends GrantedAuthority> authorities) {
		return toAuthoritySet(authorities).equals(Set.of(EMPLOYEE));
	}

	/**
	 * Determines whether the given authorities contain exclusively the user role.
	 * <p>
	 * This method returns {@code true} only if the set of authorities contains exactly
	 * one role and that role is {@code USER}.
	 * </p>
	 *
	 * @param authorities the authorities to inspect
	 * @return {@code true} if the only role present is user; {@code false} otherwise
	 */
	public static boolean hasOnlyUserRole(Collection<? extends GrantedAuthority> authorities) {
		return toAuthoritySet(authorities).equals(Set.of(USER));
	}

	private static boolean hasRole(Collection<? extends GrantedAuthority> authorities, String role) {
		return authorities.stream().anyMatch(authority -> role.equals(authority.getAuthority()));
	}

	private static Set<String> toAuthoritySet(Collection<? extends GrantedAuthority> authorities) {
		return authorities.stream()
				.map(GrantedAuthority::getAuthority)
				.collect(Collectors.toUnmodifiableSet());
	}

	private static String authority(Role role) {
		return "ROLE_" + role.name();
	}

}