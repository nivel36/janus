/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.policy;

import es.nivel36.janus.security.Actor;

/**
 * Decides whether an authenticated actor may perform an action in a given
 * context.
 *
 * @param <C> the minimum context needed to make the authorization decision
 */
@FunctionalInterface
public interface Policy<C> {

	/**
	 * Evaluates permission for an operation without changing the actor or context.
	 *
	 * @param  actor                nonnull provisioned caller with recognized roles
	 * @param  context              minimum operation context; its nullability and
	 *                              invariants are defined by the concrete policy
	 * @return                      true when the actor satisfies this policy for
	 *                              the context, false otherwise
	 * @throws NullPointerException if actor is null or a required context is null
	 */
	boolean allows(Actor actor, C context);
}
