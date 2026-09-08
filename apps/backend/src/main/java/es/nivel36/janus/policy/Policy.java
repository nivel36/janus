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

	boolean allows(Actor actor, C context);
}
